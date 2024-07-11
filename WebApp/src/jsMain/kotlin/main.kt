import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import dev.inmo.tgbotapi.webapps.cloud.*
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

    window.onload = {
        val scope = CoroutineScope(Dispatchers.Default)
        runCatching {

            scope.launchSafelyWithoutExceptions {
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
                appendText("Answer in chat button")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})
            document.body ?.appendText("Allow to write in private messages: ${webApp.initDataUnsafe.user ?.allowsWriteToPM ?: "User unavailable"}")

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

            document.body ?.appendElement("p", {})

            document.body ?.appendElement("button") {
                addEventListener("click", { webApp.requestWriteAccess() })
                appendText("Request write access without callback")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("button") {
                addEventListener("click", { webApp.requestWriteAccess { document.body ?.log("Write access request result: $it") } })
                appendText("Request write access with callback")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

            document.body ?.appendElement("button") {
                addEventListener("click", { webApp.requestContact() })
                appendText("Request contact without callback")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("button") {
                addEventListener("click", { webApp.requestContact { document.body ?.log("Contact request result: $it") } })
                appendText("Request contact with callback")
            } ?: window.alert("Unable to load body")

            document.body ?.appendElement("p", {})

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
                    textContent = "Header color: ${hex.value.uppercase()} (click to change)"
                }
                addEventListener("click", {
                    updateHeaderColor()
                })
                updateHeaderColor()
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
