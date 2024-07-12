import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDeepLinks
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDeepLink
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.formatting.makeTelegramDeepLink
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource

/**
 * This bot will send you deeplink to this bot when you send some text message and react on the `start` button
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
        onDeepLink { (it, deepLink) ->
            reply(it, "Ok, I got deep link \"${deepLink}\" in trigger")
        }
        waitDeepLinks().subscribeSafelyWithoutExceptions(this) { (it, deepLink) ->
            reply(it, "Ok, I got deep link \"${deepLink}\" in waiter")
            println(triggersHolder.handleableCommandsHolder.handleable)
        }
    }.second.join()
}
