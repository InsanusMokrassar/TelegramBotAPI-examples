import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.ktor.server.createKtorServer
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.webhook.setWebhookInfo
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.includeWebhookHandlingInRoute
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.utils.buildEntities
import io.ktor.server.routing.*

/**
 * Registers a Telegram webhook and starts a blocking, plain-HTTP Ktor server for its update POSTs.
 *
 * The first argument is the bot token and the first `https://` argument is the public webhook address. The first
 * remaining non-`debug` argument becomes the optional route, the first integer becomes the port (default `8080`),
 * and an argument exactly equal to `debug` enables formatted KSLog output. A numeric port can also be selected as the
 * route because route parsing is performed independently.
 *
 * TLS termination and certificate provisioning must be handled outside this process.
 *
 * @param args positional webhook/server configuration described above
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()
    val address = args.first { it.startsWith("https://") }
    val subpath = args.drop(1).firstOrNull { it != address && it != "debug" }
    val port = args.firstNotNullOfOrNull { it.toIntOrNull() } ?: 8080
    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val bot = telegramBot(botToken)

    val behaviourContext = bot.buildBehaviour (defaultExceptionsHandler = { it.printStackTrace() }) {
        onCommand("start", initialFilter = { it.chat is PrivateChat }) {
            reply(
                it,
                buildEntities {
                    +"Url: $address" + "\n"
                    +"Listening server: 0.0.0.0" + "\n"
                    +"Listening port: $port"
                }
            )
        }

        setMyCommands(BotCommand("start", "Get webhook info"))
    }

    val webhookInfoSubpath = subpath ?.let { "/" + it.removePrefix("/") } ?: "" // drop leading `/` to add it in the beginning for correct construction of subpath
    bot.setWebhookInfo(address + webhookInfoSubpath)

    createKtorServer(
        "0.0.0.0",
        port,
    ) {
        routing {
            if (subpath == null) {
                includeWebhookHandlingInRoute(behaviourContext, block = behaviourContext.asUpdateReceiver)
            } else {
                route(subpath) {
                    includeWebhookHandlingInRoute(behaviourContext, block = behaviourContext.asUpdateReceiver)
                }
            }
        }
    }.start(true)
}
