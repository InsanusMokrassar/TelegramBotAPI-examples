import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.chat.members.restrictChatMember
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.asContentMessage
import dev.inmo.tgbotapi.extensions.utils.asPossiblyReplyMessage
import dev.inmo.tgbotapi.extensions.utils.commonMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extendedGroupChatOrNull
import dev.inmo.tgbotapi.extensions.utils.fromUserMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.restrictedChatMemberOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.whenMemberChatMember
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ChatPermissions
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    val allowedAdmin = ChatId(args[1].toLong())

    fun Boolean?.allowedSymbol() = when (this) {
        true -> "✅"
        false -> "❌"
        null -> ""
    }

    val granularDataPrefix = "granular"
    val messagesToggleGranularData = "$granularDataPrefix messages"
    val otherMessagesToggleGranularData = "$granularDataPrefix other messages"
    val audiosToggleGranularData = "$granularDataPrefix audios"
    val voicesToggleGranularData = "$granularDataPrefix voices"
    val videosToggleGranularData = "$granularDataPrefix videos"
    val videoNotesToggleGranularData = "$granularDataPrefix video notes"
    val photosToggleGranularData = "$granularDataPrefix photos"
    val webPagePreviewToggleGranularData = "$granularDataPrefix web page preview"
    val pollsToggleGranularData = "$granularDataPrefix polls"
    val documentsToggleGranularData = "$granularDataPrefix documents"

    val commonDataPrefix = "common"
    val pollsToggleCommonData = "$commonDataPrefix polls"
    val otherMessagesToggleCommonData = "$commonDataPrefix other messages"
    val webPagePreviewToggleCommonData = "$commonDataPrefix web page preview"

    suspend fun BehaviourContext.getUserChatPermissions(chatId: ChatId, userId: UserId): ChatPermissions? {
        val chatMember = getChatMember(chatId, userId)
        return chatMember.restrictedChatMemberOrNull() ?: chatMember.whenMemberChatMember {
            getChat(chatId).extendedGroupChatOrNull() ?.permissions
        }
    }

    suspend fun BehaviourContext.buildGranularKeyboard(chatId: ChatId, userId: UserId): InlineKeyboardMarkup? {
        val permissions = getUserChatPermissions(chatId, userId) ?: return null

        return inlineKeyboard {
            row {
                dataButton("Send messages${permissions.canSendMessages.allowedSymbol()}", messagesToggleGranularData)
                dataButton("Send other messages${permissions.canSendOtherMessages.allowedSymbol()}", otherMessagesToggleGranularData)
            }
            row {
                dataButton("Send audios${permissions.canSendAudios.allowedSymbol()}", audiosToggleGranularData)
                dataButton("Send voices${permissions.canSendVoiceNotes.allowedSymbol()}", voicesToggleGranularData)
            }
            row {
                dataButton("Send videos${permissions.canSendVideos.allowedSymbol()}", videosToggleGranularData)
                dataButton("Send video notes${permissions.canSendVideoNotes.allowedSymbol()}", videoNotesToggleGranularData)
            }
            row {
                dataButton("Send photos${permissions.canSendPhotos.allowedSymbol()}", photosToggleGranularData)
                dataButton("Add web preview${permissions.canAddWebPagePreviews.allowedSymbol()}", webPagePreviewToggleGranularData)
            }
            row {
                dataButton("Send polls${permissions.canSendPolls.allowedSymbol()}", pollsToggleGranularData)
                dataButton("Send documents${permissions.canSendDocuments.allowedSymbol()}", documentsToggleGranularData)
            }
        }
    }

    suspend fun BehaviourContext.buildCommonKeyboard(chatId: ChatId, userId: UserId): InlineKeyboardMarkup? {
        val permissions = getUserChatPermissions(chatId, userId) ?: return null

        return inlineKeyboard {
            row {
                dataButton("Send polls${permissions.canSendPolls.allowedSymbol()}", pollsToggleCommonData)
            }
            row {
                dataButton("Send other messages${permissions.canSendOtherMessages.allowedSymbol()}", otherMessagesToggleCommonData)
            }
            row {
                dataButton("Add web preview${permissions.canAddWebPagePreviews.allowedSymbol()}", webPagePreviewToggleCommonData)
            }
        }
    }

    bot.buildBehaviourWithLongPolling(
        defaultExceptionsHandler = {
            println(it)
        }
    ) {
        onCommand("simple", initialFilter = { it.chat is PublicChat && it.fromUserMessageOrNull() ?.user ?.id == allowedAdmin }) {
            val replyMessage = it.replyTo
            val userInReply = replyMessage ?.fromUserMessageOrNull() ?.user ?.id ?: return@onCommand
            reply(
                replyMessage,
                "Manage keyboard:",
                replyMarkup = buildCommonKeyboard(it.chat.id.toChatId(), userInReply) ?: return@onCommand
            )
        }
        onCommand("granular", initialFilter = { it.chat is PublicChat && it.fromUserMessageOrNull() ?.user ?.id == allowedAdmin }) {
            val replyMessage = it.replyTo
            val userInReply = replyMessage ?.fromUserMessageOrNull() ?.user ?.id ?: return@onCommand
            reply(
                replyMessage,
                "Manage keyboard:",
                replyMarkup = buildGranularKeyboard(it.chat.id.toChatId(), userInReply) ?: return@onCommand
            )
        }

        onMessageDataCallbackQuery(
            Regex("^${granularDataPrefix}.*"),
            initialFilter = { it.user.id == allowedAdmin }
        ) {
            val messageReply = it.message.commonMessageOrNull() ?.replyTo ?.fromUserMessageOrNull() ?: return@onMessageDataCallbackQuery
            val userId = messageReply.user.id
            val permissions = getUserChatPermissions(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            val newPermission = when (it.data) {
                messagesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendMessages = permissions.canSendMessages ?.let { !it } ?: false
                    )
                }
                otherMessagesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendOtherMessages = permissions.canSendOtherMessages ?.let { !it } ?: false
                    )
                }
                audiosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendAudios = permissions.canSendAudios ?.let { !it } ?: false
                    )
                }
                voicesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVoiceNotes = permissions.canSendVoiceNotes ?.let { !it } ?: false
                    )
                }
                videosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVideos = permissions.canSendVideos ?.let { !it } ?: false
                    )
                }
                videoNotesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVideoNotes = permissions.canSendVideoNotes ?.let { !it } ?: false
                    )
                }
                photosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendPhotos = permissions.canSendPhotos ?.let { !it } ?: false
                    )
                }
                webPagePreviewToggleGranularData -> {
                    permissions.copyGranular(
                        canAddWebPagePreviews = permissions.canAddWebPagePreviews ?.let { !it } ?: false
                    )
                }
                pollsToggleGranularData -> {
                    permissions.copyGranular(
                        canSendPolls = permissions.canSendPolls ?.let { !it } ?: false
                    )
                }
                documentsToggleGranularData -> {
                    permissions.copyGranular(
                        canSendDocuments = permissions.canSendDocuments ?.let { !it } ?: false
                    )
                }
                else -> permissions.copyGranular()
            }

            restrictChatMember(
                it.message.chat.id,
                userId,
                permissions = newPermission,
                useIndependentChatPermissions = true
            )

            edit(
                it.message,
                replyMarkup = buildGranularKeyboard(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            )
        }

        onMessageDataCallbackQuery(
            Regex("^${commonDataPrefix}.*"),
            initialFilter = { it.user.id == allowedAdmin }
        ) {
            val messageReply = it.message.commonMessageOrNull() ?.replyTo ?.fromUserMessageOrNull() ?: return@onMessageDataCallbackQuery
            val userId = messageReply.user.id
            val permissions = getUserChatPermissions(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            val newPermission = when (it.data) {
                pollsToggleCommonData -> {
                    permissions.copyCommon(
                        canSendPolls = permissions.canSendPolls ?.let { !it } ?: false
                    )
                }
                otherMessagesToggleCommonData -> {
                    permissions.copyCommon(
                        canSendOtherMessages = permissions.canSendOtherMessages ?.let { !it } ?: false
                    )
                }
                webPagePreviewToggleCommonData -> {
                    permissions.copyCommon(
                        canAddWebPagePreviews = permissions.canAddWebPagePreviews ?.let { !it } ?: false
                    )
                }
                else -> permissions.copyCommon()
            }

            restrictChatMember(
                it.message.chat.id,
                userId,
                permissions = newPermission,
                useIndependentChatPermissions = false
            )

            edit(
                it.message,
                replyMarkup = buildCommonKeyboard(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            )
        }

        setMyCommands(
            BotCommand("simple", "Trigger simple keyboard. Use with reply to user"),
            BotCommand("granular", "Trigger granular keyboard. Use with reply to user"),
            scope = BotCommandScope.AllGroupChats
        )
    }.join()
}
