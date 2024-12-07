import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import dev.inmo.tgbotapi.webapps.cloud.*
import dev.inmo.tgbotapi.webapps.events.*
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
import kotlinx.dom.clear
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.*
import kotlin.random.Random
import kotlin.random.nextUBytes

fun HTMLElement.log(text: String) {
    appendText(text)
    appendElement("p", {})
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    console.log("Web app started")
    val client = HttpClient()
    val baseUrl = window.location.origin.removeSuffix("/")

    renderComposable("root") {
        val scope = rememberCoroutineScope()
        val isSafeState = remember { mutableStateOf<Boolean?>(null) }
        val logsState = remember { mutableStateListOf<String>() }
        LaunchedEffect(baseUrl) {
            val response = client.post("$baseUrl/check") {
                setBody(
                    Json.encodeToString(
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

        Text(
            when (isSafeState.value) {
                null -> "Checking safe state..."
                true -> "Data is safe"
                false -> "Data is unsafe"
            }
        )
        Text(webApp.initDataUnsafe.chat.toString())

        Button({
            onClick {
                scope.launchSafelyWithoutExceptions {
                    handleResult({ "Clicked" }) {
                        client.post("${window.location.origin.removeSuffix("/")}/inline") {
                            parameter(webAppQueryIdField, it)
                            setBody(TextContent("Clicked", ContentType.Text.Plain))
                            logsState.add(url.build().toString())
                        }.coroutineContext.job.join()
                    }
                }
            }
        }) {
            Text("Answer in chat button")
        }

        P()
        Text("Allow to write in private messages: ${webApp.initDataUnsafe.user ?.allowsWriteToPM ?: "User unavailable"}")

        P()
        Text("Alerts:")
        Button({
            onClick {
                webApp.showPopup(
                    PopupParams(
                        "It is sample title of default button",
                        "It is sample message of default button",
                        DefaultPopupButton("default", "Default button"),
                        OkPopupButton("ok"),
                        DestructivePopupButton("destructive", "Destructive button")
                    )
                ) {
                    logsState.add(
                        when (it) {
                            "default" -> "You have clicked default button in popup"
                            "ok" -> "You have clicked ok button in popup"
                            "destructive" -> "You have clicked destructive button in popup"
                            else -> "I can't imagine where you take button with id $it"
                        }
                    )
                }
            }
        }) {
            Text("Popup")
        }
        Button({
            onClick {
                webApp.showAlert(
                    "This is alert message"
                ) {
                    logsState.add(
                        "You have closed alert"
                    )
                }
            }
        }) {
            Text("Alert")
        }

        P()
        Button({
            onClick {
                webApp.requestWriteAccess()
            }
        }) {
            Text("Request write access without callback")
        }
        Button({
            onClick {
                webApp.requestWriteAccess {
                    logsState.add("Write access request result: $it")
                }
            }
        }) {
            Text("Request write access with callback")
        }

        P()
        Button({
            onClick {
                webApp.requestContact()
            }
        }) {
            Text("Request contact without callback")
        }
        Button({
            onClick {
                webApp.requestContact { logsState.add("Contact request result: $it") }
            }
        }) {
            Text("Request contact with callback")
        }
        P()

        logsState.forEach {
            P { Text(it) }
        }
    }

    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {

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

            document.body ?.appendElement("button") {
                fun updateHeaderColor() {
                    val (r, g, b) = Random.nextUBytes(3)
                    val hex = Color.Hex(r, g, b)
                    webApp.setHeaderColor(hex)
                    (this as? HTMLButtonElement) ?.style ?.backgroundColor = hex.value
                    textContent = "Header color: ${webApp.headerColor ?.uppercase()} (click to change)"
                }
                addEventListener("click", {
                    updateHeaderColor()
                })
                updateHeaderColor()
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            document.body ?.appendElement("button") {
                fun updateBackgroundColor() {
                    val (r, g, b) = Random.nextUBytes(3)
                    val hex = Color.Hex(r, g, b)
                    webApp.setBackgroundColor(hex)
                    (this as? HTMLButtonElement) ?.style ?.backgroundColor = hex.value
                    textContent = "Background color: ${webApp.backgroundColor ?.uppercase()} (click to change)"
                }
                addEventListener("click", {
                    updateBackgroundColor()
                })
                updateBackgroundColor()
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            document.body ?.appendElement("button") {
                fun updateBottomBarColor() {
                    val (r, g, b) = Random.nextUBytes(3)
                    val hex = Color.Hex(r, g, b)
                    webApp.setBottomBarColor(hex)
                    (this as? HTMLButtonElement) ?.style ?.backgroundColor = hex.value
                    textContent = "Bottom bar color: ${webApp.bottomBarColor ?.uppercase()} (click to change)"
                }
                addEventListener("click", {
                    updateBottomBarColor()
                })
                updateBottomBarColor()
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            fun Element.updateCloudStorageContent() {
                clear()
                webApp.cloudStorage.getAll {
                    it.onSuccess {
                        document.body ?.log(it.toString())
                        appendElement("label") { textContent = "Cloud storage" }

                        appendElement("p", {})

                        it.forEach { (k, v) ->
                            appendElement("div") {
                                val kInput = appendElement("input", {}) as HTMLInputElement
                                val vInput = appendElement("input", {}) as HTMLInputElement

                                kInput.value = k.key
                                vInput.value = v.value

                                appendElement("button") {
                                    addEventListener("click", {
                                        if (k.key == kInput.value) {
                                            webApp.cloudStorage.set(k.key, vInput.value) {
                                                document.body ?.log(it.toString())
                                                this@updateCloudStorageContent.updateCloudStorageContent()
                                            }
                                        } else {
                                            webApp.cloudStorage.remove(k.key) {
                                                it.onSuccess {
                                                    webApp.cloudStorage.set(kInput.value, vInput.value) {
                                                        document.body ?.log(it.toString())
                                                        this@updateCloudStorageContent.updateCloudStorageContent()
                                                    }
                                                }
                                            }
                                        }
                                    })
                                    this.textContent = "Save"
                                }
                            }

                            appendElement("p", {})
                        }
                        appendElement("label") { textContent = "Cloud storage: add new" }

                        appendElement("p", {})

                        appendElement("div") {
                            val kInput = appendElement("input", {}) as HTMLInputElement

                            appendElement("button") {
                                textContent = "Add key"
                                addEventListener("click", {
                                    webApp.cloudStorage.set(kInput.value, kInput.value) {
                                        document.body ?.log(it.toString())
                                        this@updateCloudStorageContent.updateCloudStorageContent()
                                    }
                                })
                            }
                        }

                        appendElement("p", {})
                    }.onFailure {
                        document.body ?.log(it.stackTraceToString())
                    }
                }
            }
            val cloudStorageContentDiv = document.body ?.appendElement("div") {} as HTMLDivElement

            document.body ?.appendElement("p", {})

            webApp.apply {
                onThemeChanged {
                    document.body ?.log("Theme changed: ${webApp.themeParams}")
                }
                onViewportChanged {
                    document.body ?.log("Viewport changed: ${it}")
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
                secondaryButton.apply {
                    setText("Secondary button")
                    onClick {
                        document.body ?.log("Secondary button clicked")
                        hapticFeedback.notificationOccurred(
                            HapticFeedbackType.Warning
                        )
                    }
                    show()
                }
                onSettingsButtonClicked {
                    document.body ?.log("Settings button clicked")
                }
                onWriteAccessRequested {
                    document.body ?.log("Write access request result: $it")
                }
                onContactRequested {
                    document.body ?.log("Contact request result: $it")
                }
            }
            webApp.ready()
            document.body ?.appendElement("input", {
                (this as HTMLInputElement).value = window.location.href
            })
            cloudStorageContentDiv.updateCloudStorageContent()
        }.onFailure {
            window.alert(it.stackTraceToString())
        }
    }
}
