import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.ktor.server.createKtorServer
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.set.setUserEmojiStatus
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onWriteAccessAllowed
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.webAppButton
import dev.inmo.tgbotapi.requests.answers.InlineQueryResultsButton
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.webapps.WebAppInfo
import dev.inmo.tgbotapi.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Accepts two parameters:
 *
 * * Telegram Token
 * * URL where will be placed
 * * Port (default 8080)
 *
 * Will start the server to share the static (index.html and WebApp.js) on 0.0.0.0:8080
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val telegramBotAPIUrlsKeeper = TelegramAPIUrlsKeeper(
        args.first(),
        testServer = args.any { it == "testServer" }
    )
    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }
    val initiationLogger = KSLog("Initialization")

    val bot = telegramBot(telegramBotAPIUrlsKeeper)
    createKtorServer(
        "0.0.0.0",
        args.getOrNull(2) ?.toIntOrNull() ?: 8080
    ) {
        routing {
            val baseJsFolder = File("WebApp/build/dist/js/")
            val prodSubFolder = File(baseJsFolder, "productionExecutable")
            val devSubFolder = File(baseJsFolder, "developmentExecutable")

            val staticFolder = when {
                prodSubFolder.exists() -> {
                    initiationLogger.i("Folder for static is ${prodSubFolder.absolutePath}")
                    prodSubFolder
                }
                devSubFolder.exists() -> {
                    initiationLogger.i("Folder for static is ${devSubFolder.absolutePath}")
                    devSubFolder
                }
                else -> error("""
                    Unable to detect any folder with static. Current working directory: ${File("").absolutePath}.
                    Searched paths:
                    * ${prodSubFolder.absolutePath}
                    * ${devSubFolder.absolutePath}
                """.trimIndent())
            }

            staticFiles("", staticFolder) {
                default("${staticFolder.absolutePath}${File.separator}index.html")
            }
            post("inline") {
                val requestBody = call.receiveText()
                val queryId = call.parameters[webAppQueryIdField] ?.let(::InlineQueryId) ?: error("$webAppQueryIdField should be presented")

                bot.answerInlineQuery(queryId, listOf(InlineQueryResultArticle(queryId, "Result", InputTextMessageContent(requestBody))))
                call.respond(HttpStatusCode.OK)
            }
            post("check") {
                val requestBody = call.receiveText()
                val webAppCheckData = Json.decodeFromString(WebAppDataWrapper.serializer(), requestBody)

                val isSafe = telegramBotAPIUrlsKeeper.checkWebAppData(webAppCheckData.data, webAppCheckData.hash)

                call.respond(HttpStatusCode.OK, isSafe.toString())
            }
            post("setCustomEmoji") {
                val requestBody = call.receiveText()
                val webAppCheckData = Json.decodeFromString(WebAppDataWrapper.serializer(), requestBody)

                val isSafe = telegramBotAPIUrlsKeeper.checkWebAppData(webAppCheckData.data, webAppCheckData.hash)
                val rawUserId = call.parameters[userIdField] ?.toLongOrNull() ?.let(::RawChatId) ?: error("$userIdField should be presented as long value")

                val set = if (isSafe) {
                    runCatching {
                        bot.setUserEmojiStatus(
                            UserId(rawUserId),
                            CustomEmojiIdToSet
                        )
                    }.getOrElse { false }
                } else {
                    false
                }

                call.respond(HttpStatusCode.OK, set.toString())
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
                },
                linkPreviewOptions = LinkPreviewOptions.Small(
                    args[1],
                    showAboveText = false
                )
            )
        }
        onCommand("attachment_menu") {
            reply(
                it,
                "Button",
                replyMarkup = inlineKeyboard {
                    row {
                        webAppButton("Open WebApp", WebAppInfo(args[1]))
                    }
                },
                linkPreviewOptions = LinkPreviewOptions.Large(
                    args[1],
                    showAboveText = true
                )
            )
        }
        onBaseInlineQuery {
            answerInlineQuery(
                it,
                button = InlineQueryResultsButton.invoke(
                    "Open webApp",
                    WebAppInfo(args[1])
                )
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
        onWriteAccessAllowed(initialFilter = { it.chatEvent.webAppName != null }) {
            send(it.chat, "Thanks for adding ${it.chatEvent.webAppName} to the attachment menu")
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
