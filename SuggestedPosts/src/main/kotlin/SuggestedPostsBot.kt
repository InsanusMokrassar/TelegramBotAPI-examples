import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.getMyStarBalance
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.resend
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.suggested.approveSuggestedPost
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitSuggestedPostApproved
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitSuggestedPostDeclined
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChannelDirectMessagesConfigurationChanged
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostApprovalFailed
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostApproved
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostDeclined
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostPaid
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onSuggestedPostRefunded
import dev.inmo.tgbotapi.extensions.utils.channelDirectMessagesContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.previewChannelDirectMessagesChatOrNull
import dev.inmo.tgbotapi.extensions.utils.suggestedChannelDirectMessagesContentMessageOrNull
import dev.inmo.tgbotapi.types.message.SuggestedPostParameters
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.firstOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * This place can be the playground for your code.
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.Default),
        testServer = isTestServer,
    ) {
        // start here!!
        val me = getMe()
        println(me)

        onCommand("start") {
            println(getChat(it.chat))
        }

        onContentMessage {
            val message = it.channelDirectMessagesContentMessageOrNull() ?: return@onContentMessage
            val chat = getChat(it.chat)
            println(chat)

            resend(
                message.chat.id,
                message.content,
                suggestedPostParameters = SuggestedPostParameters()
            )
        }

        onContentMessage(
            subcontextUpdatesFilter = { _, _ -> true } // important to not miss updates in channel for waitSuggestedPost events
        ) { message ->
            val suggestedPost = message.suggestedChannelDirectMessagesContentMessageOrNull() ?: return@onContentMessage
            val chat = getChat(message.chat)

            firstOf(
                {
                    waitSuggestedPostApproved().filter {
                        it.suggestedPostMessage ?.chat ?.id == message.chat.id
                    }.first()
                },
                {
                    waitSuggestedPostDeclined().filter {
                        it.suggestedPostMessage ?.chat ?.id == message.chat.id
                    }.first()
                },
                {
                    for (i in 0 until 3) {
                        delay(1000L)
                        send(suggestedPost.chat, "${3 - i}")
                    }
                    approveSuggestedPost(suggestedPost)
                },
            )
        }

        onSuggestedPostPaid {
            println(it)
            reply(it, "Paid")
        }
        onSuggestedPostApproved {
            println(it)
            reply(it, "Approved")
        }
        onSuggestedPostDeclined {
            println(it)
            reply(it, "Declined")
        }
        onSuggestedPostRefunded {
            println(it)
            reply(it, "Refunded")
        }
        onSuggestedPostApprovalFailed {
            println(it)
            reply(it, "Approval failed")
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
