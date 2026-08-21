import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCompleted
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayWinners
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Starts a long-polling bot that prints its profile and giveaway-related updates.
 *
 * The first argument must be the bot token. The optional, case-sensitive `debug`
 * and `testServer` flags enable diagnostic logging and the Bot API test environment.
 *
 * @param args bot token followed by any optional flags
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(botToken, testServer = isTestServer) {
        // start here!!
        val me = getMe()
        println(me)

        onGiveawayCreated {
            println(it)
        }

        onGiveawayCompleted {
            println(it)
        }

        onGiveawayWinners {
            println(it)
        }

        onGiveawayContent {
            println(it)
        }

//        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
//            println(it)
//        }
    }.second.join()
}
