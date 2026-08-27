import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs [activateKeyboardsBot] on the JVM and prints its startup bot information to standard output.
 *
 * @param args the bot token as the first element and, optionally, `debug` in a later element to enable formatted
 * KSLog output
 */
suspend fun main(args: Array<String>) {
    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    withContext(Dispatchers.IO) { // IO for inheriting of it in side of activateKeyboardsBot
        activateKeyboardsBot(args.first()) {
            println(it)
        }
    }
}
