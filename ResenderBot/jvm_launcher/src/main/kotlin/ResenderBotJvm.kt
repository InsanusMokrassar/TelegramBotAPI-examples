import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog

/**
 * Starts the JVM resender launcher.
 *
 * [args] must contain the bot token first. An optional exact `debug` value in the
 * second position enables diagnostic logging; later elements are ignored.
 */
suspend fun main(args: Array<String>) {
    val isDebug = args.getOrNull(1) == "debug"

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    activateResenderBot(args.first()) {
        println(it)
    }
}
