import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.common.Percentage
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.business.deleteBusinessMessages
import dev.inmo.tgbotapi.extensions.api.business.readBusinessMessage
import dev.inmo.tgbotapi.extensions.api.business.removeBusinessAccountProfilePhoto
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountBio
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountName
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountProfilePhoto
import dev.inmo.tgbotapi.extensions.api.business.setBusinessAccountUsername
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.modify.pinChatMessage
import dev.inmo.tgbotapi.extensions.api.chat.modify.unpinChatMessage
import dev.inmo.tgbotapi.extensions.api.files.downloadFileToTemp
import dev.inmo.tgbotapi.extensions.api.get.getBusinessConnection
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.stories.postStory
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.commonMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extendedPrivateChatOrThrow
import dev.inmo.tgbotapi.extensions.utils.ifAccessibleMessage
import dev.inmo.tgbotapi.extensions.utils.ifBusinessContentMessage
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.requests.abstracts.multipartFile
import dev.inmo.tgbotapi.requests.business_connection.InputProfilePhoto
import dev.inmo.tgbotapi.requests.stories.PostStory
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.business_connection.BusinessConnectionId
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.stories.InputStoryContent
import dev.inmo.tgbotapi.types.stories.StoryArea
import dev.inmo.tgbotapi.types.stories.StoryAreaPosition
import dev.inmo.tgbotapi.types.stories.StoryAreaType
import dev.inmo.tgbotapi.utils.botCommand
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
                if (reset) {
                    +"Account bio has been reset"
                } else {
                    +"Account bio has not been set. Set it manually: " + code(initialBio)
                }
            }
        }
        suspend fun handleSetProfilePhoto(it: CommonMessage<TextContent>, isPublic: Boolean) {
            val businessConnectionId = chatsBusinessConnections[it.chat.id] ?: return@handleSetProfilePhoto
            val replyTo = it.replyTo ?.commonMessageOrNull() ?.withContentOrNull<PhotoContent>()
            if (replyTo == null) {
                reply(it) {
                    +"Reply to photo for using of this command"
                }
                return@handleSetProfilePhoto
            }

            val set = runCatching {
                val file = downloadFileToTemp(replyTo.content)
                setBusinessAccountProfilePhoto(
                    businessConnectionId,
                    InputProfilePhoto.Static(
                        file.multipartFile()
                    ),
                    isPublic = isPublic
                )
            }.getOrElse {
                it.printStackTrace()
                false
            }
            reply(it) {
                if (set) {
                    +"Account profile photo has been set. It will be reset within 15 seconds"
                } else {
                    +"Account profile photo has not been set"
                }
            }
            if (set == false) { return@handleSetProfilePhoto }
            delay(15.seconds)
            val reset = runCatching {
                removeBusinessAccountProfilePhoto(
                    businessConnectionId,
                    isPublic = isPublic
                )
            }.getOrElse {
                it.printStackTrace()
                false
            }
            reply(it) {
                if (reset) {
                    +"Account profile photo has been reset"
                } else {
                    +"Account profile photo has not been set. Set it manually"
                }
            }
        }
        onCommand("set_business_account_profile_photo", initialFilter = { it.chat is PrivateChat }) {
            handleSetProfilePhoto(it, false)
        }
        onCommand("set_business_account_profile_photo_public", initialFilter = { it.chat is PrivateChat }) {
            handleSetProfilePhoto(it, true)
        }

        onCommand("postStory", initialFilter = { it.chat is PrivateChat }) {
            val businessConnectionId = chatsBusinessConnections[it.chat.id] ?: return@onCommand
            val replyTo = it.replyTo ?.commonMessageOrNull() ?.withContentOrNull<PhotoContent>()
            if (replyTo == null) {
                reply(it) {
                    +"Reply to photo for using of this command"
                }
                return@onCommand
            }

            val set = runCatching {
                val file = downloadFileToTemp(replyTo.content)
                postStory(
                    businessConnectionId,
                    InputStoryContent.Photo(
                        file.multipartFile()
                    ),
                    activePeriod = PostStory.ACTIVE_PERIOD_6_HOURS,
                    areas = listOf(
                        StoryArea(
                            StoryAreaPosition(
                                x = Percentage.of100(50.0),
                                y = Percentage.of100(50.0),
                                width = Percentage.of100(8.0),
                                height = Percentage.of100(8.0),
                                rotationAngle = 45.0,
                                cornerRadius = Percentage.of100(4.0),
                            ),
                            StoryAreaType.Link(
                                "https://github.com/InsanusMokrassar/TelegramBotAPI-examples/blob/master/BusinessConnectionsBot/src/main/kotlin/BusinessConnectionsBot.kt"
                            )
                        )
                    )
                ) {
                    +"It is test of postStory :)"
                }
            }.getOrElse {
                it.printStackTrace()
                null
            }
            reply(it) {
                if (set != null) {
                    +"Story has been posted. You may unpost it with " + botCommand("remove_story")
                } else {
                    +"Story has not been posted"
                }
            }
        }
    }.second.join()
}