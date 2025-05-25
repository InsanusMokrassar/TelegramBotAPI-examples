import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.business.deleteBusinessMessages
import dev.inmo.tgbotapi.extensions.api.business.readBusinessMessage
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountBio
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountName
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountUsername
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.modify.pinChatMessage
import dev.inmo.tgbotapi.extensions.api.chat.modify.unpinChatMessage
import dev.inmo.tgbotapi.extensions.api.get.getBusinessConnection
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extendedPrivateChatOrThrow
import dev.inmo.tgbotapi.extensions.utils.ifAccessibleMessage
import dev.inmo.tgbotapi.extensions.utils.ifBusinessContentMessage
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.business_connection.BusinessConnectionId
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.utils.code
import dev.inmo.tgbotapi.utils.row
import korlibs.time.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

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
    val chatsBusinessConnections = mutableMapOf<ChatId, BusinessConnectionId>()
    val businessConnectionsChatsMutex = Mutex()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val me = getMe()
        println(me)

        onBusinessConnectionEnabled {
            businessConnectionsChatsMutex.withLock {
                businessConnectionsChats[it.id] = it.userChatId
                chatsBusinessConnections[it.userChatId] = it.id
            }
            send(it.userChatId, "Business connection ${it.businessConnectionId.string} has been enabled")
        }
        onBusinessConnectionDisabled {
            businessConnectionsChatsMutex.withLock {
                businessConnectionsChats.remove(it.id)
                chatsBusinessConnections.remove(it.userChatId)
            }
            send(it.userChatId, "Business connection ${it.businessConnectionId.string} has been disabled")
        }

        onContentMessage {
            it.ifBusinessContentMessage { businessContentMessage ->
                if (businessContentMessage.content.textContentOrNull() ?.text ?.startsWith("/pin") == true) {
                    businessContentMessage.replyTo ?.ifAccessibleMessage {
                        pinChatMessage(it)
                        return@ifBusinessContentMessage
                    }
                }
                if (businessContentMessage.content.textContentOrNull() ?.text ?.startsWith("/unpin") == true) {
                    businessContentMessage.replyTo ?.ifAccessibleMessage {
                        unpinChatMessage(it)
                        return@ifBusinessContentMessage
                    }
                }
                val sent = execute(it.content.createResend(businessContentMessage.from.id))
                if (businessContentMessage.sentByBusinessConnectionOwner) {
                    reply(sent, "You have sent this message to the ${businessContentMessage.businessConnectionId.string} related chat")
                } else {
                    reply(
                        to = sent,
                        text = "User have sent this message to you in the ${businessContentMessage.businessConnectionId.string} related chat",
                    )
                    send(
                        chatId = businessConnectionsChats[it.businessConnectionId] ?: return@ifBusinessContentMessage,
                        text = "User have sent this message to you in the ${businessContentMessage.businessConnectionId.string} related chat",
                        replyMarkup = inlineKeyboard {
                            row {
                                dataButton("Read message", "read ${it.chat.id.chatId.long} ${it.messageId.long}")
                                dataButton("Delete message", "delete ${it.chat.id.chatId.long} ${it.messageId.long}")
                            }
                        }
                    )
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
        onCommand("get_business_account_info", initialFilter = { it.chat is PrivateChat }) {
            val businessConnectionId = chatsBusinessConnections[it.chat.id]
            val businessConnectionInfo = businessConnectionId ?.let { getBusinessConnection(it) }
            reply(it) {
                if (businessConnectionInfo == null) {
                    +"There is no business connection for current chat"
                } else {
                    +(Json { prettyPrint = true; encodeDefaults = true }.encodeToString(businessConnectionInfo))
                }
            }
        }
        onMessageDataCallbackQuery(Regex("read \\d+ \\d+")) {
            val (_, chatIdString, messageIdString) = it.data.split(" ")
            val chatId = chatIdString.toLongOrNull() ?.let(::RawChatId) ?.let(::ChatId) ?: return@onMessageDataCallbackQuery
            val messageId = messageIdString.toLongOrNull() ?.let(::MessageId) ?: return@onMessageDataCallbackQuery
            val businessConnectionId = chatsBusinessConnections[it.message.chat.id]

            val readResponse = businessConnectionId ?.let { readBusinessMessage(it, chatId, messageId) }
            answer(
                it,
                if (readResponse == null) {
                    "There is no business connection for current chat"
                } else {
                    "Message has been read"
                }
            )
        }
        onMessageDataCallbackQuery(Regex("delete \\d+ \\d+")) {
            val (_, chatIdString, messageIdString) = it.data.split(" ")
            val chatId = chatIdString.toLongOrNull() ?.let(::RawChatId) ?.let(::ChatId) ?: return@onMessageDataCallbackQuery
            val messageId = messageIdString.toLongOrNull() ?.let(::MessageId) ?: return@onMessageDataCallbackQuery
            val businessConnectionId = chatsBusinessConnections[it.message.chat.id]

            val readResponse = businessConnectionId ?.let { deleteBusinessMessages(it, listOf(messageId)) }
            answer(
                it,
                if (readResponse == null) {
                    "There is no business connection for current chat"
                } else {
                    "Message has been deleted"
                }
            )
        }
        onCommandWithArgs("set_business_account_name", initialFilter = { it.chat is PrivateChat }) { it, args ->
            val firstName = args[0]
            val secondName = args.getOrNull(1)
            val businessConnectionId = chatsBusinessConnections[it.chat.id] ?: return@onCommandWithArgs
            val set = runCatching {
                setBusinessAccountName(
                    businessConnectionId,
                    firstName,
                    secondName
                )
            }.getOrElse { false }
            reply(it) {
                if (set) {
                    +"Account name has been set"
                } else {
                    +"Account name has not been set"
                }
            }
        }
        onCommandWithArgs("set_business_account_username", initialFilter = { it.chat is PrivateChat }) { it, args ->
            val username = args[0]
            val businessConnectionId = chatsBusinessConnections[it.chat.id] ?: return@onCommandWithArgs
            val set = runCatching {
                setBusinessAccountUsername(
                    businessConnectionId,
                    username
                )
            }.getOrElse {
                it.printStackTrace()
                false
            }
            reply(it) {
                if (set) {
                    +"Account username has been set"
                } else {
                    +"Account username has not been set"
                }
            }
        }
        onCommand("set_business_account_bio", requireOnlyCommandInMessage = false, initialFilter = { it.chat is PrivateChat }) {
            val initialBio = getChat(it.chat).extendedPrivateChatOrThrow().bio
            val bio = it.content.text.removePrefix("/set_business_account_bio").trim()
            val businessConnectionId = chatsBusinessConnections[it.chat.id] ?: return@onCommand
            val set = runCatching {
                setBusinessAccountBio(
                    businessConnectionId,
                    bio
                )
            }.getOrElse {
                it.printStackTrace()
                false
            }
            reply(it) {
                if (set) {
                    +"Account bio has been set. It will be reset within 15 seconds.\n\nInitial bio: " + code(initialBio)
                } else {
                    +"Account bio has not been set"
                }
            }
            delay(15.seconds)
            val reset = runCatching {
                setBusinessAccountBio(
                    businessConnectionId,
                    initialBio
                )
            }.getOrElse {
                it.printStackTrace()
                false
            }
            reply(it) {
                if (set) {
                    +"Account bio has been reset"
                } else {
                    +"Account bio has not been set. Set it manually: " + code(initialBio)
                }
            }
        }
    }.second.join()
}