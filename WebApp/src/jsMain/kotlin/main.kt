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
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.Color as ComposeColor
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
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

        Text(window.location.href)
        P()

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

            if (dataIsSafe) {
                isSafeState.value = true
                logsState.add("Data is safe")
            } else {
                isSafeState.value = false
                logsState.add("Data is unsafe")
            }

            logsState.add(
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

        Button({
            onClick {
                webApp.showConfirm(
                    "This is confirm message"
                ) {
                    logsState.add(
                        "You have pressed \"${if (it) "Ok" else "Cancel"}\" in confirm"
                    )
                }
            }
        }) {
            Text("Confirm")
        }

        P()

        val isClosingConfirmationEnabledState = remember { mutableStateOf(webApp.isClosingConfirmationEnabled) }
        Button({
            onClick {
                webApp.toggleClosingConfirmation()
                isClosingConfirmationEnabledState.value = webApp.isClosingConfirmationEnabled
            }
        }) {
            Text(
                if (isClosingConfirmationEnabledState.value) {
                    "Disable closing confirmation"
                } else {
                    "Enable closing confirmation"
                }
            )
        }

        P()

        val headerColor = remember { mutableStateOf<Color.Hex>(Color.Hex("#000000")) }
        fun updateHeaderColor() {
            val (r, g, b) = Random.nextUBytes(3)
            headerColor.value = Color.Hex(r, g, b)
            webApp.setHeaderColor(headerColor.value)
        }
        DisposableEffect(0) {
            updateHeaderColor()
            onDispose {  }
        }
        Button({
            style {
                backgroundColor(ComposeColor(headerColor.value.value))
            }
            onClick {
                updateHeaderColor()
            }
        }) {
            key(headerColor.value) {
                Text("Header color: ${webApp.headerColor ?.uppercase()} (click to change)")
            }
        }

        P()

        val backgroundColor = remember { mutableStateOf<Color.Hex>(Color.Hex("#000000")) }
        fun updateBackgroundColor() {
            val (r, g, b) = Random.nextUBytes(3)
            backgroundColor.value = Color.Hex(r, g, b)
            webApp.setBackgroundColor(backgroundColor.value)
        }
        DisposableEffect(0) {
            updateBackgroundColor()
            onDispose {  }
        }
        Button({
            style {
                backgroundColor(ComposeColor(backgroundColor.value.value))
            }
            onClick {
                updateBackgroundColor()
            }
        }) {
            key(backgroundColor.value) {
                Text("Background color: ${webApp.backgroundColor ?.uppercase()} (click to change)")
            }
        }

        P()

        val bottomBarColor = remember { mutableStateOf<Color.Hex>(Color.Hex("#000000")) }
        fun updateBottomBarColor() {
            val (r, g, b) = Random.nextUBytes(3)
            bottomBarColor.value = Color.Hex(r, g, b)
            webApp.setBottomBarColor(bottomBarColor.value)
        }
        DisposableEffect(0) {
            updateBottomBarColor()
            onDispose {  }
        }
        Button({
            style {
                backgroundColor(ComposeColor(bottomBarColor.value.value))
            }
            onClick {
                updateBottomBarColor()
            }
        }) {
            key(bottomBarColor.value) {
                Text("Bottom bar color: ${webApp.bottomBarColor ?.uppercase()} (click to change)")
            }
        }

        P()

        val storageTrigger = remember { mutableStateOf<List<Pair<CloudStorageKey, CloudStorageValue>>>(emptyList()) }
        fun updateCloudStorage() {
            webApp.cloudStorage.getAll {
                it.onSuccess {
                    storageTrigger.value = it.toList().sortedBy { it.first.key }
                }
            }
        }
        key(storageTrigger.value) {
            storageTrigger.value.forEach { (key, value) ->
                val keyState = remember { mutableStateOf(key.key) }
                val valueState = remember { mutableStateOf(value.value) }
                Input(InputType.Text) {
                    value(key.key)
                    onInput { keyState.value = it.value }
                }
                Input(InputType.Text) {
                    value(value.value)
                    onInput { valueState.value = it.value }
                }
                Button({
                    onClick {
                        if (key.key != keyState.value) {
                            webApp.cloudStorage.remove(key)
                        }
                        webApp.cloudStorage.set(keyState.value, valueState.value)
                        updateCloudStorage()
                    }
                }) {
                    Text("Save")
                }
            }
            let { // new element adding
                val keyState = remember { mutableStateOf("") }
                val valueState = remember { mutableStateOf("") }
                Input(InputType.Text) {
                    onInput { keyState.value = it.value }
                }
                Input(InputType.Text) {
                    onInput { valueState.value = it.value }
                }
                Button({
                    onClick {
                        webApp.cloudStorage.set(keyState.value, valueState.value)
                        updateCloudStorage()
                    }
                }) {
                    Text("Save")
                }
            }
        }

        remember {
            webApp.apply {

                onThemeChanged {
                    logsState.add("Theme changed: ${webApp.themeParams}")
                }
                onViewportChanged {
                    logsState.add("Viewport changed: ${it}")
                }
                backButton.apply {
                    onClick {
                        logsState.add("Back button clicked")
                        hapticFeedback.impactOccurred(
                            HapticFeedbackStyle.Heavy
                        )
                    }
                    show()
                }
                mainButton.apply {
                    setText("Main button")
                    onClick {
                        logsState.add("Main button clicked")
                        hapticFeedback.notificationOccurred(
                            HapticFeedbackType.Success
                        )
                    }
                    show()
                }
                secondaryButton.apply {
                    setText("Secondary button")
                    onClick {
                        logsState.add("Secondary button clicked")
                        hapticFeedback.notificationOccurred(
                            HapticFeedbackType.Warning
                        )
                    }
                    show()
                }
                onSettingsButtonClicked {
                    logsState.add("Settings button clicked")
                }
                onWriteAccessRequested {
                    logsState.add("Write access request result: $it")
                }
                onContactRequested {
                    logsState.add("Contact request result: $it")
                }
            }
        }

        logsState.forEach {
            P { Text(it) }
        }
    }
}
