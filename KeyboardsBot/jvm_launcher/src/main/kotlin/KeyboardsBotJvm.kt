import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
