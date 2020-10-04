import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot

/**
 * This is one of the most easiest bot - it will just print information about itself
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    println(bot.getMe())
}