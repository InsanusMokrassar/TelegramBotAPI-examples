import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.api.send.media.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterExcludeMediaGroups
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.MessageFilterByChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.possiblyWithEffectMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.shortcuts.*
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.abstracts.BusinessContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.quoteEntitiesField
import dev.inmo.tgbotapi.utils.extensions.threadIdOrNull
import kotlinx.coroutines.*

suspend fun activateResenderBot(
    token: String,
    print: (Any) -> Unit
) {
    telegramBotWithBehaviourAndLongPolling(token, scope = CoroutineScope(currentCoroutineContext() + SupervisorJob())) {
        onContentMessage(
            subcontextUpdatesFilter = MessageFilterByChat,
            initialFilter = { it !is BusinessContentMessage<*> || !it.sentByBusinessConnectionOwner }
        ) {
            val chat = it.chat

            val answer = withTypingAction(chat) {
                executeUnsafe(
                    it.content.createResend(
                        chat.id,
                        messageThreadId = it.threadIdOrNull,
                        replyParameters = it.replyInfo ?.messageMeta ?.let { meta ->
                            val quote = it.withContentOrNull<TextContent>() ?.content ?.quote
                            ReplyParameters(
                                meta,
                                entities = quote ?.textSources ?: emptyList(),
                                quotePosition = quote ?.position
                            )
                        },
                        effectId = it.possiblyWithEffectMessageOrNull() ?.effectId
                    )
                ) {
                    it.forEach(print)
                }
            }

            println("Answer info: $answer")
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
        print(bot.getMe())
    }.second.join()
}
