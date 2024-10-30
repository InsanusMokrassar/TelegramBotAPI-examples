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
 * Launches webhook-based simple bot. Required arguments:
 *
 * 1. Token
 * *. Arguments starting with `https://`
 *
 * Optional arguments:
 *
 * *. Any argument == `debug` to enable debug mode
 * *. Any argument **not** starting with `https://` and **not** equal to `debug` as **subpath** (will be used as
 * subroute to place listening of webhooks)
 * *. Any argument as number of port
 *
 * Sample: `TOKEN https://sample.com it/is/subpath 8080` will result to:
 *
 * * `TOKEN` used as token
 * * Bot will set up its webhook info as `https://sample.com/it/is/subpath`
 * * Bot will set up to listen webhooks on route `it/is/subpath`
 * * Bot will start to listen any incoming request on port `8080` and url `0.0.0.0`
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
