import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.firstOf
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.chat.members.promoteChannelAdministrator
import dev.inmo.tgbotapi.extensions.api.chat.members.restrictChatMember
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitChatSharedEventsMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommandMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitUserSharedEventsMessages
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.chat.ChatPermissions
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.ChatCommonAdministratorRights
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

sealed interface UserRetrievingStep : State {
    data class RetrievingChannelChatState(
        override val context: ChatId
    ) : UserRetrievingStep
    data class RetrievingUserIdChatState(
        override val context: ChatId,
        val channelId: ChatId
    ) : UserRetrievingStep
    data class RetrievingChatInfoDoneState(
        override val context: ChatId,
        val channelId: ChatId,
        val userId: UserId
    ) : UserRetrievingStep
}

@OptIn(PreviewFeature::class)
suspend fun main(args: Array<String>) {
    val botToken = args.first()

    val isDebug = args.getOrNull(2) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val bot = telegramBot(botToken)

    val allowedAdmin = ChatId(RawChatId(args[1].toLong()))

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

    val adminRightsDataPrefix = "admin"
    val refreshAdminRightsData = "${adminRightsDataPrefix}_refresh"
    val postMessagesToggleAdminRightsData = "${adminRightsDataPrefix}_post_messages"
    val editMessagesToggleAdminRightsData = "${adminRightsDataPrefix}_edit_messages"
    val deleteMessagesToggleAdminRightsData = "${adminRightsDataPrefix}_delete_messages"
    val editStoriesToggleAdminRightsData = "${adminRightsDataPrefix}_edit_stories"
    val deleteStoriesToggleAdminRightsData = "${adminRightsDataPrefix}_delete_stories"
    val postStoriesToggleAdminRightsData = "${adminRightsDataPrefix}_post_stories"

    suspend fun BehaviourContext.getUserChatPermissions(chatId: ChatId, userId: UserId): ChatPermissions? {
        val chatMember = getChatMember(chatId, userId)
        return chatMember.restrictedMemberChatMemberOrNull() ?: chatMember.whenMemberChatMember {
            getChat(chatId).extendedGroupChatOrNull() ?.permissions
        }
    }
    fun buildGranularKeyboard(
        permissions: ChatPermissions
    ): InlineKeyboardMarkup {
        return inlineKeyboard {
            row {
                dataButton("Send messages${permissions.canSendMessages.allowedSymbol()}", messagesToggleGranularData)
                dataButton(
                    "Send other messages${permissions.canSendOtherMessages.allowedSymbol()}",
                    otherMessagesToggleGranularData
                )
            }
            row {
                dataButton("Send audios${permissions.canSendAudios.allowedSymbol()}", audiosToggleGranularData)
                dataButton("Send voices${permissions.canSendVoiceNotes.allowedSymbol()}", voicesToggleGranularData)
            }
            row {
                dataButton("Send videos${permissions.canSendVideos.allowedSymbol()}", videosToggleGranularData)
                dataButton(
                    "Send video notes${permissions.canSendVideoNotes.allowedSymbol()}",
                    videoNotesToggleGranularData
                )
            }
            row {
                dataButton("Send photos${permissions.canSendPhotos.allowedSymbol()}", photosToggleGranularData)
                dataButton(
                    "Add web preview${permissions.canAddWebPagePreviews.allowedSymbol()}",
                    webPagePreviewToggleGranularData
                )
            }
            row {
                dataButton("Send polls${permissions.canSendPolls.allowedSymbol()}", pollsToggleGranularData)
                dataButton("Send documents${permissions.canSendDocuments.allowedSymbol()}", documentsToggleGranularData)
            }
        }
    }
    fun buildAdminRightsKeyboard(
        permissions: AdministratorChatMember?,
        channelId: ChatId,
        userId: UserId
    ): InlineKeyboardMarkup {
        return inlineKeyboard {
            permissions ?.also {
                row {
                    dataButton("Refresh", "$refreshAdminRightsData ${channelId.chatId} ${userId.chatId}")
                }
                row {
                    dataButton("Edit messages${permissions.canEditMessages.allowedSymbol()}", "$editMessagesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                    dataButton("Delete messages${permissions.canRemoveMessages.allowedSymbol()}", "$deleteMessagesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                }
                row {
                    dataButton("Post messages${permissions.canPostMessages.allowedSymbol()}", "$postMessagesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                }
                row {
                    dataButton("Edit stories${permissions.canEditStories.allowedSymbol()}", "$editStoriesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                    dataButton("Delete stories${permissions.canDeleteStories.allowedSymbol()}", "$deleteStoriesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                }
                row {
                    dataButton("Post stories${permissions.canPostStories.allowedSymbol()}", "$postStoriesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
                }
            } ?: row {
                dataButton("Promote to admin", "$postMessagesToggleAdminRightsData ${channelId.chatId} ${userId.chatId}")
            }
        }
    }

    suspend fun BehaviourContext.buildGranularKeyboard(chatId: ChatId, userId: UserId): InlineKeyboardMarkup? {
        return buildGranularKeyboard(
            getUserChatPermissions(chatId, userId) ?: return null
        )
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

    bot.buildBehaviourWithFSMAndStartLongPolling<UserRetrievingStep>(
        defaultExceptionsHandler = {
            it.printStackTrace()
        },
    ) {
        onCommand(
            "simple",
            initialFilter = { it.chat is PublicChat && it.fromUserMessageOrNull()?.user?.id == allowedAdmin }
        ) {
            val replyMessage = it.replyTo
            val userInReply = replyMessage?.fromUserMessageOrNull()?.user?.id ?: return@onCommand
            if (replyMessage is AccessibleMessage) {
                reply(
                    replyMessage,
                    "Manage keyboard:",
                    replyMarkup = buildCommonKeyboard(it.chat.id.toChatId(), userInReply) ?: return@onCommand
                )
            } else {
                reply(it) {
                    regular("Reply to somebody's message to get hist/her rights keyboard")
                }
            }
        }
        onCommand(
            "granular",
            initialFilter = {
                it.chat is ChannelChat || (it.chat is PublicChat && it.fromUserMessageOrNull()?.user?.id == allowedAdmin)
            }
        ) {
            val replyMessage = it.replyTo
            val userInReply = replyMessage?.fromUserMessageOrNull()?.user?.id ?: return@onCommand

            if (replyMessage is AccessibleMessage) {
                reply(
                    replyMessage,
                    "Manage keyboard:",
                    replyMarkup = buildGranularKeyboard(it.chat.id.toChatId(), userInReply) ?: return@onCommand
                )
            } else {
                reply(it) {
                    regular("Reply to somebody's message to get hist/her rights keyboard")
                }
            }
        }

        onMessageDataCallbackQuery(
            Regex("^${granularDataPrefix}.*"),
            initialFilter = { it.user.id == allowedAdmin }
        ) {
            val messageReply =
                it.message.commonMessageOrNull()?.replyTo?.fromUserMessageOrNull() ?: return@onMessageDataCallbackQuery
            val userId = messageReply.user.id
            val permissions =
                getUserChatPermissions(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            val newPermission = when (it.data) {
                messagesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendMessages = permissions.canSendMessages?.let { !it } ?: false
                    )
                }

                otherMessagesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendOtherMessages = permissions.canSendOtherMessages?.let { !it } ?: false
                    )
                }

                audiosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendAudios = permissions.canSendAudios?.let { !it } ?: false
                    )
                }

                voicesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVoiceNotes = permissions.canSendVoiceNotes?.let { !it } ?: false
                    )
                }

                videosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVideos = permissions.canSendVideos?.let { !it } ?: false
                    )
                }

                videoNotesToggleGranularData -> {
                    permissions.copyGranular(
                        canSendVideoNotes = permissions.canSendVideoNotes?.let { !it } ?: false
                    )
                }

                photosToggleGranularData -> {
                    permissions.copyGranular(
                        canSendPhotos = permissions.canSendPhotos?.let { !it } ?: false
                    )
                }

                webPagePreviewToggleGranularData -> {
                    permissions.copyGranular(
                        canAddWebPagePreviews = permissions.canAddWebPagePreviews?.let { !it } ?: false
                    )
                }

                pollsToggleGranularData -> {
                    permissions.copyGranular(
                        canSendPolls = permissions.canSendPolls?.let { !it } ?: false
                    )
                }

                documentsToggleGranularData -> {
                    permissions.copyGranular(
                        canSendDocuments = permissions.canSendDocuments?.let { !it } ?: false
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
                replyMarkup = buildGranularKeyboard(it.message.chat.id.toChatId(), userId)
                    ?: return@onMessageDataCallbackQuery
            )
        }

        onMessageDataCallbackQuery(
            Regex("^${commonDataPrefix}.*"),
            initialFilter = { it.user.id == allowedAdmin }
        ) {
            val messageReply =
                it.message.commonMessageOrNull()?.replyTo?.fromUserMessageOrNull() ?: return@onMessageDataCallbackQuery
            val userId = messageReply.user.id
            val permissions =
                getUserChatPermissions(it.message.chat.id.toChatId(), userId) ?: return@onMessageDataCallbackQuery
            val newPermission = when (it.data) {
                pollsToggleCommonData -> {
                    permissions.copyCommon(
                        canSendPolls = permissions.canSendPolls?.let { !it } ?: false
                    )
                }

                otherMessagesToggleCommonData -> {
                    permissions.copyCommon(
                        canSendOtherMessages = permissions.canSendOtherMessages?.let { !it } ?: false
                    )
                }

                webPagePreviewToggleCommonData -> {
                    permissions.copyCommon(
                        canAddWebPagePreviews = permissions.canAddWebPagePreviews?.let { !it } ?: false
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
                replyMarkup = buildCommonKeyboard(it.message.chat.id.toChatId(), userId)
                    ?: return@onMessageDataCallbackQuery
            )
        }

        onMessageDataCallbackQuery(
            Regex("^${adminRightsDataPrefix}.*"),
            initialFilter = { it.user.id == allowedAdmin }
        ) {
            val (channelIdString, userIdString) = it.data.split(" ").drop(1)
            val channelId = ChatId(RawChatId(channelIdString.toLong()))
            val userId = ChatId(RawChatId(userIdString.toLong()))
            val chatMember = getChatMember(channelId, userId)
            val asAdmin = chatMember.administratorChatMemberOrNull()

            val realData = it.data.takeWhile { it != ' ' }

            fun Boolean?.toggleIfData(data: String) = if (realData == data) {
                !(this ?: false)
            } else {
                null
            }

            if (realData != refreshAdminRightsData) {
                promoteChannelAdministrator(
                    channelId,
                    userId,
                    canPostMessages = asAdmin ?.canPostMessages.toggleIfData(postMessagesToggleAdminRightsData),
                    canEditMessages = asAdmin ?.canEditMessages.toggleIfData(editMessagesToggleAdminRightsData),
                    canDeleteMessages = asAdmin ?.canRemoveMessages.toggleIfData(deleteMessagesToggleAdminRightsData),
                    canEditStories = asAdmin ?.canEditStories.toggleIfData(editStoriesToggleAdminRightsData),
                    canDeleteStories = asAdmin ?.canDeleteStories.toggleIfData(deleteStoriesToggleAdminRightsData),
                    canPostStories = asAdmin ?.canPostStories.toggleIfData(postStoriesToggleAdminRightsData),
                )
            }

            edit(
                it.message,
                replyMarkup = buildAdminRightsKeyboard(
                    getChatMember(
                        channelId,
                        userId
                    ).administratorChatMemberOrNull(),
                    channelId,
                    userId
                )
            )
        }

        strictlyOn<UserRetrievingStep.RetrievingChannelChatState> { state ->
            val requestId = RequestId.random()
            send(
                state.context,
                replyMarkup = replyKeyboard(
                    oneTimeKeyboard = true,
                    resizeKeyboard = true
                ) {
                    row {
                        requestChatButton(
                            "Choose channel",
                            requestId = requestId,
                            isChannel = true,
                            botIsMember = true,
                            botRightsInChat = ChatCommonAdministratorRights(
                                canPromoteMembers = true,
                                canRestrictMembers = true
                            ),
                            userRightsInChat = ChatCommonAdministratorRights(
                                canPromoteMembers = true,
                                canRestrictMembers = true
                            )
                        )
                    }
                }
            ) {
                regular("Ok, send me the channel in which you wish to manage user, or use ")
                botCommand("cancel")
                regular(" to cancel the request")
            }
            firstOf {
                include {
                    val chatId = waitChatSharedEventsMessages().mapNotNull {
                        it.chatEvent.chatId.takeIf { _ ->
                            it.chatEvent.requestId == requestId && it.sameChat(state.context)
                        }
                    }.first()
                    UserRetrievingStep.RetrievingUserIdChatState(state.context, chatId)
                }
                include {
                    waitCommandMessage("cancel").filter { it.sameChat(state.context) }.first()
                    null
                }
            }
        }
        strictlyOn<UserRetrievingStep.RetrievingUserIdChatState> { state ->
            val requestId = RequestId.random()
            send(
                state.context,
                replyMarkup = replyKeyboard(
                    oneTimeKeyboard = true,
                    resizeKeyboard = true
                ) {
                    row {
                        requestUserButton(
                            "Choose user",
                            requestId = requestId
                        )
                    }
                }
            ) {
                regular("Ok, send me the user for which you wish to change rights, or use ")
                botCommand("cancel")
                regular(" to cancel the request")
            }

            firstOf {
                include {
                    val userContactChatId = waitUserSharedEventsMessages().filter {
                        it.sameChat(state.context)
                    }.first().chatEvent.chatId
                    UserRetrievingStep.RetrievingChatInfoDoneState(
                        state.context,
                        state.channelId,
                        userContactChatId
                    )
                }
                include {
                    waitCommandMessage("cancel").filter { it.sameChat(state.context) }.first()
                    null
                }
            }
        }

        strictlyOn<UserRetrievingStep.RetrievingChatInfoDoneState> { state ->
            val chatMember = getChatMember(state.channelId, state.userId).administratorChatMemberOrNull()
            if (chatMember == null) {
                return@strictlyOn null
            }
            send(
                state.context,
                replyMarkup = buildAdminRightsKeyboard(
                    chatMember,
                    state.channelId,
                    state.userId
                )
            ) {
                regular("Rights of ")
                mentionln(chatMember.user)
                regular("Please, remember, that to be able to change user rights bot must promote user by itself to admin")
            }
            null
        }

        onCommand("rights_in_channel") {
            startChain(UserRetrievingStep.RetrievingChannelChatState(it.chat.id.toChatId()))
        }

        setMyCommands(
            BotCommand("simple", "Trigger simple keyboard. Use with reply to user"),
            BotCommand("granular", "Trigger granular keyboard. Use with reply to user"),
            BotCommand("rights_in_channel", "Trigger granular keyboard. Use with reply to user"),
            scope = BotCommandScope.AllGroupChats
        )

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.join()
}
