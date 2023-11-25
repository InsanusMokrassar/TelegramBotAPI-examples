import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.filter.filtered
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.utils.DefaultKTgBotAPIKSLog

/**
 * This is one of the most easiest bot - it will just print information about itself
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    setDefaultKSLog(
        KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
            println(defaultMessageFormatter(level, tag, message, throwable))
        }
    )
    val bot = telegramBot(botToken)

    println(bot.getMe())
}
