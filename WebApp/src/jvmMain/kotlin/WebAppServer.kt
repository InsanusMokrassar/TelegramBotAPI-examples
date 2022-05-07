import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.ktor.server.createKtorServer
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.types.webapps.WebAppInfo
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Accepts two parameters:
 *
 * * Telegram Token
 * * URL where will be placed
 *
 * Will start the server to share the static (index.html and WebApp.js) on 0.0.0.0:8080
 */
suspend fun main(vararg args: String) {
    val bot = telegramBot(args.first(), testServer = args.any { it == "testServer" })
    createKtorServer(
        "0.0.0.0",
        8080,
        additionalEngineEnvironmentConfigurator = {
            parentCoroutineContext += Dispatchers.IO
        }
    ) {
        routing {
            static {
                files(File("WebApp/build/distributions"))
                default("WebApp/build/distributions/index.html")
            }
            post("inline") {
                val requestBody = call.receiveText()
                val queryId = call.parameters[webAppQueryIdField] ?: error("$webAppQueryIdField should be presented")

                bot.answer(queryId, InlineQueryResultArticle(queryId, "Result", InputTextMessageContent(requestBody)))
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(false)

    bot.buildBehaviourWithLongPolling(
        defaultExceptionsHandler = { it.printStackTrace() }
    ) {
        onCommand("reply_markup") {
            reply(
                it,
                "Button",
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        webAppButton("Open WebApp", WebAppInfo(args[1]))
                    }
                }

            )
        }
        onCommand("inline") {
            reply(
                it,
                "Button",
                replyMarkup = inlineKeyboard {
                    row {
                        webAppButton("Open WebApp", WebAppInfo(args[1]))
                    }
                }

            )
        }
        onUnhandledCommand {
            println("Unhandled command: ${it.content}")
        }
        setMyCommands(
            BotCommand("reply_markup", "Use to get reply markup keyboard with web app trigger"),
            BotCommand("inline", "Use to get inline keyboard with web app trigger"),
        )
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
        println(getMe())
    }.join()
}
