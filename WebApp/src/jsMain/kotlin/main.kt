import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import org.w3c.dom.HTMLElement

fun HTMLElement.log(text: String) {
    appendElement("p", {})
    appendText(text)
}

fun main() {
    console.log("Web app started")
    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {
            document.body ?.appendElement("button") {
                addEventListener("click", {
                    scope.launchSafelyWithoutExceptions {
                        handleResult({ "Clicked" }) {
                            HttpClient().post("${window.location.origin.removeSuffix("/")}/inline") {
                                parameter(webAppQueryIdField, it)
                                setBody(TextContent("Clicked", ContentType.Text.Plain))
                                document.body ?.log(url.build().toString())
                            }.coroutineContext.job.join()
                        }
                    }
                })
                appendText("Example button")
            } ?: window.alert("Unable to load body")
            webApp.apply {
                onThemeChanged {
                    document.body ?.log("Theme changed: ${webApp.themeParams}")
                }
                onViewportChanged {
                    document.body ?.log("Viewport changed: ${it.isStateStable}")
                }
            }
            webApp.ready()
        }.onFailure {
            window.alert(it.stackTraceToString())
        }
    }
}
