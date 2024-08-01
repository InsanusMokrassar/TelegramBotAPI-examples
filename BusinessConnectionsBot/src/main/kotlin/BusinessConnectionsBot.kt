import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.modify.pinChatMessage
import dev.inmo.tgbotapi.extensions.api.chat.modify.unpinChatMessage
import dev.inmo.tgbotapi.extensions.api.get.getBusinessConnection
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.accessibleMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.ifBusinessContentMessage
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.business_connection.BusinessConnectionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val isDebug = args.getOrNull(1) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val businessConnectionsChats = mutableMapOf<BusinessConnectionId, ChatId>()
    val businessConnectionsChatsMutex = Mutex()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val me = getMe()
        println(me)

        onBusinessConnectionEnabled {
            businessConnectionsChatsMutex.withLock {
                businessConnectionsChats[it.id] = it.userChatId
            }
            send(it.userChatId, "Business connection ${it.businessConnectionId.string} has been enabled")
        }
        onBusinessConnectionDisabled {
            businessConnectionsChatsMutex.withLock {
                businessConnectionsChats.remove(it.id)
            }
            send(it.userChatId, "Business connection ${it.businessConnectionId.string} has been disabled")
        }

        onContentMessage {
            it.ifBusinessContentMessage { businessContentMessage ->
                if (businessContentMessage.content.textContentOrNull() ?.text ?.startsWith("/pin") == true) {
                    pinChatMessage(businessContentMessage)
                    return@ifBusinessContentMessage
                }
                if (businessContentMessage.content.textContentOrNull() ?.text ?.startsWith("/unpin") == true) {
                    unpinChatMessage(businessContentMessage)
                    return@ifBusinessContentMessage
                }
                val sent = execute(it.content.createResend(businessContentMessage.from.id))
                if (businessContentMessage.sentByBusinessConnectionOwner) {
                    reply(sent, "You have sent this message to the ${businessContentMessage.businessConnectionId.string} related chat")
                } else {
                    reply(sent, "User have sent this message to you in the ${businessContentMessage.businessConnectionId.string} related chat")
                }
            }
        }
        onEditedContentMessage {
            it.ifBusinessContentMessage { businessContentMessage ->
                val sent = execute(businessContentMessage.content.createResend(businessContentMessage.from.id))
                if (businessContentMessage.sentByBusinessConnectionOwner) {
                    reply(sent, "You have edited this message in the ${businessContentMessage.businessConnectionId.string} related chat")
                } else {
                    reply(sent, "User have edited this message to you in the ${businessContentMessage.businessConnectionId.string} related chat")
                }
            }
        }
        onBusinessMessagesDeleted {
            var businessConnectionOwnerChat = businessConnectionsChatsMutex.withLock {
                businessConnectionsChats[it.businessConnectionId]
            }
            if (businessConnectionOwnerChat == null) {
                val businessConnection = getBusinessConnection(it.businessConnectionId)
                businessConnectionsChatsMutex.withLock {
                    businessConnectionsChats[businessConnection.businessConnectionId] = businessConnection.userChatId
                }
                businessConnectionOwnerChat = businessConnection.userChatId
            }
            send(businessConnectionOwnerChat, "There are several removed messages in chat ${it.chat.id}: ${it.messageIds}")
        }
    }.second.join()
}