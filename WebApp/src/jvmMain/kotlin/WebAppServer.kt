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
import dev.inmo.tgbotapi.utils.*
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.http.content.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.Charset

/**
 * Accepts two parameters:
 *
 * * Telegram Token
 * * URL where will be placed
 *
 * Will start the server to share the static (index.html and WebApp.js) on 0.0.0.0:8080
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val telegramBotAPIUrlsKeeper = TelegramAPIUrlsKeeper(
        args.first(),
        testServer = args.any { it == "testServer" }
    )
    val bot = telegramBot(telegramBotAPIUrlsKeeper)
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
            post("check") {
                val requestBody = call.receiveText()
                val webAppCheckData = Json {  }.decodeFromString(WebAppDataWrapper.serializer(), requestBody)

                val isSafe = telegramBotAPIUrlsKeeper.checkWebAppData(webAppCheckData.data, webAppCheckData.hash)

                call.respond(HttpStatusCode.OK, isSafe.toString())
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
            reply(
                it,
                buildEntities {
                    +"Use " + botCommand("inline") + " to get inline web app button\n"
                    +"Use " + botCommand("reply_markup") + " to get reply markup web app button\n"
                }
            )
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
