import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatShared
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUserShared
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUsersShared
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.keyboardButtonRequestUserLimit
import dev.inmo.tgbotapi.types.message.textsources.mention
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.utils.row

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

    val bot = telegramBot(botToken)

    val requestIdUserOrBot = RequestId(0)
    val requestIdUserNonPremium = RequestId(1)
    val requestIdUserAny = RequestId(2)
    val requestIdUserPremium = RequestId(3)
    val requestIdBot = RequestId(4)

    val requestIdUsersOrBots = RequestId(5)
    val requestIdUsersNonPremium = RequestId(6)
    val requestIdUsersAny = RequestId(7)
    val requestIdUsersPremium = RequestId(8)
    val requestIdBots = RequestId(9)

    val requestIdAnyChat = RequestId(10)
    val requestIdChannel = RequestId(11)
    val requestIdPublicChannel = RequestId(12)
    val requestIdPrivateChannel = RequestId(13)
    val requestIdChannelUserOwner = RequestId(14)

    val requestIdGroup = RequestId(15)
    val requestIdPublicGroup = RequestId(16)
    val requestIdPrivateGroup = RequestId(17)
    val requestIdGroupUserOwner = RequestId(18)

    val requestIdForum = RequestId(19)
    val requestIdPublicForum = RequestId(20)
    val requestIdPrivateForum = RequestId(21)
    val requestIdForumUserOwner = RequestId(22)

    val keyboard = replyKeyboard(
        resizeKeyboard = true,
    ) {
        row {
            requestUserOrBotButton(
                "\uD83D\uDC64/\uD83E\uDD16 (1)",
                requestIdUserOrBot,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestUserButton(
                "\uD83D\uDC64☆ (1)",
                requestIdUserNonPremium,
                premiumUser = false,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestUserButton(
                "\uD83D\uDC64 (1)",
                requestIdUserAny,
                premiumUser = null,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestUserButton(
                "\uD83D\uDC64★ (1)",
                requestIdUserPremium,
                premiumUser = true,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestBotButton(
                "\uD83E\uDD16 (1)",
                requestIdBot,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestUsersOrBotsButton(
                "\uD83D\uDC64/\uD83E\uDD16",
                requestIdUsersOrBots,
                maxCount = keyboardButtonRequestUserLimit.last,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestUsersButton(
                "\uD83D\uDC64☆",
                requestIdUsersNonPremium,
                premiumUser = false,
                maxCount = keyboardButtonRequestUserLimit.last,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestUsersButton(
                "\uD83D\uDC64",
                requestIdUsersAny,
                premiumUser = null,
                maxCount = keyboardButtonRequestUserLimit.last,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestUsersButton(
                "\uD83D\uDC64★",
                requestIdUsersPremium,
                premiumUser = true,
                maxCount = keyboardButtonRequestUserLimit.last,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestBotsButton(
                "\uD83E\uDD16",
                requestIdBots,
                maxCount = keyboardButtonRequestUserLimit.last,
                requestName = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestChatButton(
                "\uD83D\uDDE3/\uD83D\uDC65",
                requestIdAnyChat,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestChatButton(
                "\uD83D\uDDE3",
                requestIdChannel,
                isChannel = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestChatButton(
                "\uD83D\uDDE3\uD83D\uDD17",
                requestIdPublicChannel,
                isChannel = true,
                isPublic = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestChatButton(
                "\uD83D\uDDE3❌\uD83D\uDD17",
                requestIdPrivateChannel,
                isChannel = true,
                isPublic = false,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestChatButton(
                "\uD83D\uDDE3\uD83D\uDC6E",
                requestIdChannelUserOwner,
                isChannel = true,
                isOwnedBy = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestGroupButton(
                "👥",
                requestIdGroup,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "👥\uD83D\uDD17",
                requestIdPublicGroup,
                isPublic = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "👥❌\uD83D\uDD17",
                requestIdPrivateGroup,
                isPublic = false,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "👥\uD83D\uDC6E",
                requestIdGroupUserOwner,
                isOwnedBy = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
        row {
            requestGroupButton(
                "🏛",
                requestIdForum,
                isForum = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "🏛\uD83D\uDD17",
                requestIdPublicForum,
                isPublic = true,
                isForum = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "🏛❌\uD83D\uDD17",
                requestIdPrivateForum,
                isPublic = false,
                isForum = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
            requestGroupButton(
                "🏛\uD83D\uDC6E",
                requestIdForumUserOwner,
                isOwnedBy = true,
                isForum = true,
                requestTitle = true,
                requestUsername = true,
                requestPhoto = true
            )
        }
    }

    bot.buildBehaviourWithLongPolling (defaultExceptionsHandler = { it.printStackTrace() }) {
        onCommand("start", initialFilter = { it.chat is PrivateChat }) {
            reply(
                it,
                "Here possible requests buttons:",
                replyMarkup = keyboard
            )
        }

        onUsersShared {
            it.chatEvent.userIds.forEach { userId ->
                val userInfo = runCatchingSafely { getChat(userId) }.getOrNull()
                reply(
                    it,
                ) {
                    +"You have shared "
                    +mention(
                        when (it.chatEvent.requestId) {
                            requestIdUserOrBot -> "user or bot"
                            requestIdUserNonPremium -> "non premium user"
                            requestIdUserAny -> "any user"
                            requestIdUserPremium -> "premium user"
                            requestIdBot -> "bot"
                            else -> "somebody O.o"
                        },
                        userId
                    )
                    +" (user info: $userInfo; user id: $userId)"
                }
            }
        }

        onChatShared {
            val chatId = it.chatEvent.chatId
            val chatInfo = runCatchingSafely { getChat(chatId) }.getOrNull()
            reply(
                it,
            ) {
                +"You have shared "
                +when (it.chatEvent.requestId) {
                    requestIdAnyChat -> "some chat"
                    requestIdChannel -> "any channel"
                    requestIdPublicChannel -> "public channel"
                    requestIdPrivateChannel -> "private channel"
                    requestIdChannelUserOwner -> "channel owned by you"
                    requestIdGroup -> "any group"
                    requestIdPublicGroup -> "public group"
                    requestIdPrivateGroup -> "private group"
                    requestIdGroupUserOwner -> "group owned by you"
                    requestIdForum -> "any forum"
                    requestIdPublicForum -> "public forum"
                    requestIdPrivateForum -> "private forum"
                    requestIdForumUserOwner -> "forum owned by you"
                    else -> "some chat O.o"
                }
                +" (chat info: $chatInfo; chat id: $chatId)"
            }
        }

        setMyCommands(BotCommand("start", "Trigger buttons"))
    }.join()
}
