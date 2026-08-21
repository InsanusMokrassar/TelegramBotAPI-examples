import kotlinx.browser.document
import kotlinx.coroutines.*
import org.w3c.dom.*

private val scope = CoroutineScope(Dispatchers.Default)

/**
 * Installs the browser token form and starts one resender for each submission.
 * Callback output from each instance is displayed in its associated page element.
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
                        activateResenderBot(token) {
                            infoDiv.innerHTML = it.toString()
                        }
                    }
                }

                false
            }
        }
    )
}
