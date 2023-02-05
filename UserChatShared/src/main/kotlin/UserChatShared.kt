import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatShared
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUserShared
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestBotButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestChatButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestGroupButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestUserButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestUserOrBotButton
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.textsources.mention
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.utils.row

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    val requestIdUserOrBot = RequestId(0)
    val requestIdUserNonPremium = RequestId(1)
    val requestIdUserAny = RequestId(2)
    val requestIdUserPremium = RequestId(3)
    val requestIdBot = RequestId(4)

    val requestIdAnyChat = RequestId(5)
    val requestIdChannel = RequestId(6)
    val requestIdPublicChannel = RequestId(7)
    val requestIdPrivateChannel = RequestId(8)
    val requestIdChannelUserOwner = RequestId(9)

    val requestIdGroup = RequestId(10)
    val requestIdPublicGroup = RequestId(11)
    val requestIdPrivateGroup = RequestId(12)
    val requestIdGroupUserOwner = RequestId(13)

    val requestIdForum = RequestId(14)
    val requestIdPublicForum = RequestId(15)
    val requestIdPrivateForum = RequestId(16)
    val requestIdForumUserOwner = RequestId(17)

    val keyboard = replyKeyboard(
        resizeKeyboard = true,
    ) {
        row {
            requestUserOrBotButton(
                "\uD83D\uDC64/\uD83E\uDD16",
                requestIdUserOrBot
            )
        }
        row {
            requestUserButton(
                "\uD83D\uDC64☆",
                requestIdUserNonPremium,
                premiumUser = false
            )
            requestUserButton(
                "\uD83D\uDC64",
                requestIdUserAny,
                premiumUser = null
            )
            requestUserButton(
                "\uD83D\uDC64★",
                requestIdUserPremium,
                premiumUser = true
            )
            requestBotButton(
                "\uD83E\uDD16",
                requestIdBot
            )
        }
        row {
            requestChatButton(
                "\uD83D\uDDE3/\uD83D\uDC65",
                requestIdAnyChat
            )
        }
        row {
            requestChatButton(
                "\uD83D\uDDE3",
                requestIdChannel,
                isChannel = true
            )
            requestChatButton(
                "\uD83D\uDDE3\uD83D\uDD17",
                requestIdPublicChannel,
                isChannel = true,
                isPublic = true
            )
            requestChatButton(
                "\uD83D\uDDE3❌\uD83D\uDD17",
                requestIdPrivateChannel,
                isChannel = true,
                isPublic = false
            )
            requestChatButton(
                "\uD83D\uDDE3\uD83D\uDC6E",
                requestIdChannelUserOwner,
                isChannel = true,
                isOwnedBy = true
            )
        }
        row {
            requestGroupButton(
                "👥",
                requestIdGroup
            )
            requestGroupButton(
                "👥\uD83D\uDD17",
                requestIdPublicGroup,
                isPublic = true
            )
            requestGroupButton(
                "👥❌\uD83D\uDD17",
                requestIdPrivateGroup,
                isPublic = false
            )
            requestGroupButton(
                "👥\uD83D\uDC6E",
                requestIdGroupUserOwner,
                isOwnedBy = true
            )
        }
        row {
            requestGroupButton(
                "🏛",
                requestIdForum,
                isForum = true
            )
            requestGroupButton(
                "🏛\uD83D\uDD17",
                requestIdPublicForum,
                isPublic = true,
                isForum = true
            )
            requestGroupButton(
                "🏛❌\uD83D\uDD17",
                requestIdPrivateForum,
                isPublic = false,
                isForum = true
            )
            requestGroupButton(
                "🏛\uD83D\uDC6E",
                requestIdForumUserOwner,
                isOwnedBy = true,
                isForum = true
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

        onUserShared {
            val userId = it.chatEvent.userId
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
    }.join()
}
