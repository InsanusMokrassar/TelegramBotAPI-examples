import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.ktor.server.createKtorServer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import io.ktor.application.call
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.tomcat.Tomcat
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
    createKtorServer(
        Tomcat,
        "0.0.0.0",
        8080,
        additionalEngineEnvironmentConfigurator = {
            parentCoroutineContext += Dispatchers.IO
        }
    ) {
        routing {
            static {
                files(File("WebApp/build/distributions"))
            }
        }
    }.start(false)

    telegramBotWithBehaviourAndLongPolling(
        args.first(),
        defaultExceptionsHandler = { it.printStackTrace() }
    ) {
        onCommand("start") {
            reply(
                it,
                "Button:",
                replyMarkup = inlineKeyboard {
                    row {
                        webAppButton("Open", args[1])
                    }
                }
            )
        }
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
        println(getMe())
    }.second.join()
}
