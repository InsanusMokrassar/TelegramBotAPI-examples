import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import io.ktor.client.HttpClient
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

fun main() {
    console.log("Web app started")
    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {
            document.body ?.appendElement("button") {
                (this as HTMLElement).onclick = {
                    scope.launchSafelyWithoutExceptions {
                        HttpClient().post<HttpResponse>("${window.location.origin.removeSuffix("/")}/inline") {
                            parameter(webAppQueryIdField, webApp.initDataUnsafe.queryId)
                            body = "Clicked"
                            contentType(ContentType.Text.Plain)
                        }
                        handleResult(
                            { "Clicked" }
                        ) {
                            HttpClient().post<HttpResponse>(
                                "${window.location.origin}/inline"
                            ) {
                                parameter(webAppQueryIdField, it)
                                body = "Clicked"
                            }
                        }
                    }
                }
                appendText("Example button")
            } ?: window.alert("Unable to load body")
            webApp.apply {
                onThemeChanged {
                    document.body ?.appendText("Theme changed: ${webApp.themeParams}")
                    document.body ?.appendElement("p", {})
                }
                onViewportChanged {
                    document.body ?.appendText("Viewport changed: ${it.isStateStable}")
                    document.body ?.appendElement("p", {})
                }
            }
            webApp.ready()
        }.onFailure {
            window.alert(it.stackTraceToString())
        }
    }
}
