import dev.inmo.tgbotapi.webapps.*
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.encodeURLPath
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.appendElement
import kotlinx.dom.appendText

fun main() {
    console.log("Web app started")
    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {
            document.body ?.appendElement("button") {
                addEventListener(
                    "click",
                    {
                        webApp.sendData("Clicked")
                    }
                )
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
