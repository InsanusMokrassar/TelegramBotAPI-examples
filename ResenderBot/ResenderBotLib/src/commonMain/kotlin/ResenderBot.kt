import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.filter.filtered
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.withTypingAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.MessageFilterByChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.possiblyWithEffectMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.abstracts.BusinessContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.DefaultKTgBotAPIKSLog
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext

/**
 * Starts the shared long-polling resender and suspends until polling stops.
 *
 * Eligible content is recreated in its source chat with reply/quote metadata and
 * message effects preserved. Business messages sent by the business-connection
 * owner are skipped.
 *
 * @param token bot token used for polling and API requests.
 * @param print output callback for bot information and resend diagnostics.
 */
suspend fun activateResenderBot(
    token: String,
    print: (Any) -> Unit
) {
    telegramBotWithBehaviourAndLongPolling(
        token,
        scope = CoroutineScope(currentCoroutineContext() + SupervisorJob()),
    ) {
        onContentMessage(
            subcontextUpdatesFilter = MessageFilterByChat,
            initialFilter = { it !is BusinessContentMessage<*> || !it.sentByBusinessConnectionOwner }
        ) {
            val chat = it.chat

            val answer = withTypingAction(chat) {
                executeUnsafe(
                    it.content.createResend(
                        chat.id,
                        replyParameters = it.replyInfo?.messageMeta?.let { meta ->
                            val quote = it.withContentOrNull<TextContent>()?.content?.quote
                            ReplyParameters(
                                meta,
                                entities = quote?.textSources ?: emptyList(),
                                quotePosition = quote?.position
                            )
                        },
                        effectId = it.possiblyWithEffectMessageOrNull()?.effectId
                    )
                ) {
                    it.forEach(print)
                }
            }

            println("Answer info: $answer")
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(scope = this) {
            println(it)
        }
        print(bot.getMe())
    }.second.join()
}
