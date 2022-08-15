import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import dev.inmo.tgbotapi.webapps.haptic.HapticFeedbackStyle
import dev.inmo.tgbotapi.webapps.haptic.HapticFeedbackType
import dev.inmo.tgbotapi.webapps.popup.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement

fun HTMLElement.log(text: String) {
    appendText(text)
    appendElement("p", {})
}

fun main() {
    console.log("Web app started")
    val client = HttpClient()
    val baseUrl = window.location.origin.removeSuffix("/")

    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {

            scope.launchSafelyWithoutExceptions {
                val response = client.post("$baseUrl/check") {
                    setBody(
                        Json { }.encodeToString(
                            WebAppDataWrapper.serializer(),
                            WebAppDataWrapper(webApp.initData, webApp.initDataUnsafe.hash)
                        )
                    )
                }
                val dataIsSafe = response.bodyAsText().toBoolean()

                document.body ?.log(
                    if (dataIsSafe) {
                        "Data is safe"
                    } else {
                        "Data is unsafe"
                    }
                )

                document.body ?.log(
                    webApp.initDataUnsafe.chat.toString()
                )
            }

            document.body ?.appendElement("button") {
                addEventListener("click", {
                    scope.launchSafelyWithoutExceptions {
                        handleResult({ "Clicked" }) {
                            client.post("${window.location.origin.removeSuffix("/")}/inline") {
                                parameter(webAppQueryIdField, it)
                                setBody(TextContent("Clicked", ContentType.Text.Plain))
                                document.body ?.log(url.build().toString())
                            }.coroutineContext.job.join()
                        }
                    }
                })
                appendText("Example button")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})
            document.body ?.appendText("Alerts:")

            document.body ?.appendElement("button") {
                addEventListener("click", {
                    webApp.showPopup(
                        PopupParams(
                            "It is sample title of default button",
                            "It is sample message of default button",
                            DefaultPopupButton("default", "Default button"),
                            OkPopupButton("ok"),
                            DestructivePopupButton("destructive", "Destructive button")
                        )
                    ) {
                        document.body ?.log(
                            when (it) {
                                "default" -> "You have clicked default button in popup"
                                "ok" -> "You have clicked ok button in popup"
                                "destructive" -> "You have clicked destructive button in popup"
                                else -> "I can't imagine where you take button with id $it"
                            }
                        )
                    }
                })
                appendText("Popup")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("button") {
                addEventListener("click", {
                    webApp.showAlert(
                        "This is alert message"
                    ) {
                        document.body ?.log(
                            "You have closed alert"
                        )
                    }
                })
                appendText("Alert")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("button") {
                addEventListener("click", {
                    webApp.showConfirm(
                        "This is confirm message"
                    ) {
                        document.body ?.log(
                            "You have pressed \"${if (it) "Ok" else "Cancel"}\" in confirm"
                        )
                    }
                })
                appendText("Confirm")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            document.body ?.appendElement("button") {
                fun updateText() {
                    textContent = if (webApp.isClosingConfirmationEnabled) {
                        "Disable closing confirmation"
                    } else {
                        "Enable closing confirmation"
                    }
                }
                addEventListener("click", {
                    webApp.toggleClosingConfirmation()
                    updateText()
                })
                updateText()
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            webApp.apply {
                onThemeChanged {
                    document.body ?.log("Theme changed: ${webApp.themeParams}")
                }
                onViewportChanged {
                    document.body ?.log("Viewport changed: ${it.isStateStable}")
                }
                backButton.apply {
                    onClick {
                        document.body ?.log("Back button clicked")
                        hapticFeedback.impactOccurred(
                            HapticFeedbackStyle.Heavy
                        )
                    }
                    show()
                }
                mainButton.apply {
                    setText("Main button")
                    onClick {
                        document.body ?.log("Main button clicked")
                        hapticFeedback.notificationOccurred(
                            HapticFeedbackType.Success
                        )
                    }
                    show()
                }
                onSettingsButtonClicked {
                    document.body ?.log("Settings button clicked")
                }
            }
            webApp.ready()
        }.onFailure {
            window.alert(it.stackTraceToString())
        }
    }
}
