import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCompleted
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayWinners
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * This place can be the playground for your code.
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

//        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
//            println(it)
//        }
    }.second.join()
}
