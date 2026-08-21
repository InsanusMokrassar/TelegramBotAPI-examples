import kotlinx.browser.document
import kotlinx.coroutines.*
import org.w3c.dom.*

private val scope = CoroutineScope(Dispatchers.Default)

/**
 * Installs the browser launch form after `DOMContentLoaded`.
 *
 * Every submission reads the token from `bot_token`, appends a result container under `bots_container`, and launches
 * [activateKeyboardsBot]. The result of its startup `getMe` request is rendered in that new container.
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
                        activateKeyboardsBot(token) {
                            infoDiv.innerHTML = it.toString()
                        }
                    }
                }

                false
            }
        }
    )
}
