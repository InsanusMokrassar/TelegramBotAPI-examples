import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.utils.asMessageUpdate
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here.
 * Also, this place can be the playground for your code.
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { update ->
            update.asMessageUpdate()?.let { safeMessageUpdate ->
                val user = safeMessageUpdate.data.from
                val replyMessage = if (user != null) {
                    "Oh, hi, ${user.username ?: user.firstName}!"
                } else "Oh, hi!"
                reply(to = safeMessageUpdate.data, text = replyMessage)
            }
        }
    }.second.join()
}
