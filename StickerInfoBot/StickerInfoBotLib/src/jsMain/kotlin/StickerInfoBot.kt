import kotlinx.browser.document
import kotlinx.coroutines.*
import org.w3c.dom.*

private val scope = CoroutineScope(Dispatchers.Default)

/**
 * Installs the browser token form after `DOMContentLoaded` and starts a bot for every submission.
 *
 * Each bot receives its token from `bot_token`; its startup information is appended under `bots_container`.
 */
fun main() {
    document.addEventListener(
        "DOMContentLoaded",
        {
            val botsContainer = document.getElementById("bots_container") ?: return@addEventListener

            (document.getElementById("bot_token_form") as? HTMLFormElement) ?.onsubmit = {
                (document.getElementById("bot_token") as? HTMLInputElement) ?.value ?.let { token ->
                    val botContainer = document.createElement("div") as HTMLDivElement
                    botsContainer.append(botContainer)

                    val infoDiv = document.createElement("div") as HTMLDivElement
                    botContainer.append(infoDiv)

                    scope.launch {
                        activateStickerInfoBot(token) {
                            infoDiv.innerHTML = it.toString()
                        }
                    }
                }

                false
            }
        }
    )
}
