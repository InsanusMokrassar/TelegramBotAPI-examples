import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatShareThemeBackdrops
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDeepLinks
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDeepLink
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onWriteAccessAllowed
import dev.inmo.tgbotapi.extensions.utils.formatting.makeBotInviteLink
import dev.inmo.tgbotapi.extensions.utils.formatting.makeTelegramDeepLink
import dev.inmo.tgbotapi.types.EmojiType
import dev.inmo.tgbotapi.types.chat.ChatShareTheme
import dev.inmo.tgbotapi.types.message.ChatEvents.forum.WriteAccessAllowed
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource

/**
 * Runs a long-polling bot that turns non-command text into a deep link to itself
 * and acknowledges payloads received through `/start`.
 *
 * The bot account must have a username so that its deep links can be constructed.
 *
 * @param args the bot token as the first argument; any remaining arguments are ignored
 * @throws NoSuchElementException when no bot token is supplied
 * @throws IllegalStateException when the bot account has no username
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken) {
        val me = bot.getMe()
        val username = me.username
        println(me)

        if (username == null) {
            error("Unable to start bot work: it have no username")
        }

        onText(
            initialFilter = { it.content.textSources.none { it is BotCommandTextSource } } // excluding messages with commands
        ) {
            reply(it, makeTelegramDeepLink(username, it.content.text))
        }

        onCommand("start", requireOnlyCommandInMessage = true) { // handling of `start` without args
            reply(it, "Hi :) Send me any text and I will try hard to create deeplink for you")
        }
        onCommand("invite") {
            reply(it, makeBotInviteLink(username))
        }
        onWriteAccessAllowed { message ->
            val invitation = message.chatEvent as? WriteAccessAllowed.FromInvitation ?: return@onWriteAccessAllowed
            sendMessage(message.chat, "You were invited by ${invitation.inviter.firstName}.")
        }
        onCommand("sharetheme") { message ->
            val description = when (val theme = getChat(message.chat.id).shareTheme) {
                is ChatShareTheme.Emoji -> {
                    val emojis = theme.emojis.joinToString(" ") {
                        when (it) {
                            is EmojiType.Emoji -> it.emoji
                            is EmojiType.CustomEmoji -> "custom emoji ${it.customEmojiId.string}"
                        }
                    }
                    val backdrop = theme.backgroundBackdropId?.let { id ->
                        getChatShareThemeBackdrops().backdrops.firstOrNull { it.id == id }?.name ?: id.int.toString()
                    }
                    "Emoji: $emojis\nBackdrop: ${backdrop ?: "default"}"
                }
                is ChatShareTheme.UniqueGift -> "Unique gift: ${theme.gift.name.value}"
                null -> "This chat has no share theme."
            }
            reply(message, description)
        }
        onDeepLink { (it, deepLink) ->
            reply(it, "Ok, I got deep link \"${deepLink}\" in trigger")
        }
        waitDeepLinks().subscribeLoggingDropExceptions(this) { (it, deepLink) ->
            reply(it, "Ok, I got deep link \"${deepLink}\" in waiter")
            println(triggersHolder.handleableCommandsHolder.handleable)
        }
    }.second.join()
}
