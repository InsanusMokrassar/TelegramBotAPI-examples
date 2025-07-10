import androidx.compose.runtime.*
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.tgbotapi.types.userIdField
import dev.inmo.tgbotapi.types.webAppQueryIdField
import dev.inmo.tgbotapi.webapps.*
import dev.inmo.tgbotapi.webapps.accelerometer.AccelerometerStartParams
import dev.inmo.tgbotapi.webapps.cloud.*
import dev.inmo.tgbotapi.webapps.events.*
import dev.inmo.tgbotapi.webapps.gyroscope.GyroscopeStartParams
import dev.inmo.tgbotapi.webapps.haptic.HapticFeedbackStyle
import dev.inmo.tgbotapi.webapps.haptic.HapticFeedbackType
import dev.inmo.tgbotapi.webapps.orientation.DeviceOrientationStartParams
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
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.Color as ComposeColor
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.*
import kotlin.random.Random
import kotlin.random.nextUBytes

fun HTMLElement.log(text: String) {
    appendText(text)
    appendElement("p", {})
}

private object RootStyleSheet : StyleSheet() {
    val rootClass by style {
        color(ComposeColor("var(--tg-theme-text-color)"))
        backgroundColor(ComposeColor("var(--tg-theme-bg-color)"))
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    console.log("Web app started")
    val client = HttpClient()
    val baseUrl = window.location.origin.removeSuffix("/")

    renderComposable("root") {
        Style(RootStyleSheet)
        DisposableEffect(null) {
            scopeElement.classList.add(RootStyleSheet.rootClass)

            onDispose {
                scopeElement.classList.remove(RootStyleSheet.rootClass)
            }
        }
        val scope = rememberCoroutineScope()
        val isSafeState = remember { mutableStateOf<Boolean?>(null) }
        val logsState = remember { mutableStateListOf<Any?>() }

//        Text(window.location.href)
//        P()

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
        P()
        Text("Chat from WebAppInitData: ${webApp.initDataUnsafe.chat}")

        H3 { Text("Emoji status management") }
        val emojiStatusAccessState = remember { mutableStateOf(false) }
        webApp.onEmojiStatusAccessRequested {
            emojiStatusAccessState.value = it.isAllowed
        }
        Button({
            onClick {
                webApp.requestEmojiStatusAccess()
            }
        }) {
            Text("Request custom emoji status access")
        }
        if (emojiStatusAccessState.value) {
            Button({
                onClick {
                    webApp.setEmojiStatus(CustomEmojiIdToSet/* android custom emoji id */)
                }
            }) {
                Text("Set custom emoji status")
            }
            val userId = webApp.initDataUnsafe.user ?.id
            userId ?.let { userId ->
                Button({
                    onClick {
                        scope.launchLoggingDropExceptions {
                            client.post("$baseUrl/setCustomEmoji") {
                                parameter(userIdField, userId.long)
                                setBody(
                                    Json.encodeToString(
                                        WebAppDataWrapper.serializer(),
                                        WebAppDataWrapper(webApp.initData, webApp.initDataUnsafe.hash)
                                    )
                                )
                            }
                        }
                    }
                }) {
                    Text("Set custom emoji status via bot")
                }
            }
        }
        P()

        H3 { Text("Call server method with webAppQueryIdField") }
        Button({
            onClick {
                scope.launchLoggingDropExceptions {
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

        H3 { Text("Hide keyboard") }
        val hideCountdown = remember { mutableStateOf<Int?>(null) }
        Button({
            onClick {
                hideCountdown.value = 5
            }
        }) {
            if (hideCountdown.value == null) {
                Text("Hide")
            } else {
                Text("Hide in ${hideCountdown.value} seconds")
            }
        }
        LaunchedEffect(hideCountdown.value) {
            val value = hideCountdown.value
            when {
                value == null -> return@LaunchedEffect
                value > 0 -> {
                    delay(1000)
                    hideCountdown.value = hideCountdown.value ?.minus(1)
                }
                else -> {
                    webApp.hideKeyboard()
                    hideCountdown.value = null
                }
            }
        }

        P()
        H3 { Text("User info") }
        Text("Allow to write in private messages: ${webApp.initDataUnsafe.user ?.allowsWriteToPM ?: "User unavailable"}")

        P()
        H3 { Text("Alerts") }
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
        H3 { Text("Write access callbacks") }
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
        H3 { Text("Request contact") }
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
        H3 { Text("Closing confirmation") }
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
        H3 { Text("Colors") }
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

        P()
        let {
            H3 { Text("Accelerometer") }
            val enabledState = remember { mutableStateOf(webApp.accelerometer.isStarted) }
            webApp.onAccelerometerStarted { enabledState.value = true }
            webApp.onAccelerometerStopped { enabledState.value = false }
            Button({
                onClick {
                    if (enabledState.value) {
                        webApp.accelerometer.stop {  }
                    } else {
                        webApp.accelerometer.start(AccelerometerStartParams(200))
                    }
                }
            }) {
                Text("${if (enabledState.value) "Stop" else "Start"} accelerometer")
            }
            val xState = remember { mutableStateOf(webApp.accelerometer.x) }
            val yState = remember { mutableStateOf(webApp.accelerometer.y) }
            val zState = remember { mutableStateOf(webApp.accelerometer.z) }
            fun updateValues() {
                xState.value = webApp.accelerometer.x
                yState.value = webApp.accelerometer.y
                zState.value = webApp.accelerometer.z
            }
            remember {
                updateValues()
            }

            webApp.onAccelerometerChanged {
                updateValues()
            }
            if (enabledState.value) {
                P()
                Text("x: ${xState.value}")
                P()
                Text("y: ${yState.value}")
                P()
                Text("z: ${zState.value}")
            }
        }
        P()

        let {
            H3 { Text("Gyroscope") }
            val enabledState = remember { mutableStateOf(webApp.gyroscope.isStarted) }
            webApp.onGyroscopeStarted { enabledState.value = true }
            webApp.onGyroscopeStopped { enabledState.value = false }
            Button({
                onClick {
                    if (enabledState.value) {
                        webApp.gyroscope.stop {  }
                    } else {
                        webApp.gyroscope.start(GyroscopeStartParams(200))
                    }
                }
            }) {
                Text("${if (enabledState.value) "Stop" else "Start"} gyroscope")
            }
            val xState = remember { mutableStateOf(webApp.gyroscope.x) }
            val yState = remember { mutableStateOf(webApp.gyroscope.y) }
            val zState = remember { mutableStateOf(webApp.gyroscope.z) }
            fun updateValues() {
                xState.value = webApp.gyroscope.x
                yState.value = webApp.gyroscope.y
                zState.value = webApp.gyroscope.z
            }
            remember {
                updateValues()
            }

            webApp.onGyroscopeChanged {
                updateValues()
            }
            if (enabledState.value) {
                P()
                Text("x: ${xState.value}")
                P()
                Text("y: ${yState.value}")
                P()
                Text("z: ${zState.value}")
            }
        }

        P()
        let {
            H3 { Text("Device Orientation") }
            val enabledState = remember { mutableStateOf(webApp.deviceOrientation.isStarted) }
            webApp.onDeviceOrientationStarted { enabledState.value = true }
            webApp.onDeviceOrientationStopped { enabledState.value = false }
            Button({
                onClick {
                    if (enabledState.value) {
                        webApp.deviceOrientation.stop {  }
                    } else {
                        webApp.deviceOrientation.start(DeviceOrientationStartParams(200))
                    }
                }
            }) {
                Text("${if (enabledState.value) "Stop" else "Start"} deviceOrientation")
            }
            val alphaState = remember { mutableStateOf(webApp.deviceOrientation.alpha) }
            val betaState = remember { mutableStateOf(webApp.deviceOrientation.beta) }
            val gammaState = remember { mutableStateOf(webApp.deviceOrientation.gamma) }
            fun updateValues() {
                alphaState.value = webApp.deviceOrientation.alpha
                betaState.value = webApp.deviceOrientation.beta
                gammaState.value = webApp.deviceOrientation.gamma
            }
            remember {
                updateValues()
            }

            webApp.onDeviceOrientationChanged {
                updateValues()
            }
            if (enabledState.value) {
                P()
                Text("alpha: ${alphaState.value}")
                P()
                Text("beta: ${betaState.value}")
                P()
                Text("gamma: ${gammaState.value}")
            }
        }

        P()
        H3 { Text("Cloud storage") }
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

        P()
        let { // DeviceStorage
            H3 { Text("Device storage") }
            val fieldKey = remember { mutableStateOf("") }
            val fieldValue = remember { mutableStateOf("") }
            val message = remember { mutableStateOf("") }
            Div {
                Text("Start type title of key. If value will be found in device storage, it will be shown in value input")
            }

            Input(InputType.Text) {
                placeholder("Key")
                value(fieldKey.value)
                onInput {
                    fieldKey.value = it.value
                    webApp.deviceStorage.getItem(it.value) { e, v ->
                        fieldValue.value = v ?: ""
                        if (v == null) {
                            message.value = "Value for key \"${it.value}\" has not been found"
                        } else {
                            message.value = "Value for key \"${it.value}\" has been found: \"$v\""
                        }
                    }
                }
            }
            Div {
                Text("If you want to change value if typed key - just put it here")
            }
            Input(InputType.Text) {
                placeholder("Value")
                value(fieldValue.value)
                onInput {
                    fieldValue.value = it.value
                    webApp.deviceStorage.setItem(fieldKey.value, it.value) { e, v ->
                        if (v == true) {
                            fieldValue.value = it.value
                            message.value = "Value \"${it.value}\" has been saved"
                        }
                    }
                }
            }

            if (message.value.isNotEmpty()) {
                Div { Text(message.value) }
            }
        }
        P()

        let { // DeviceStorage
            H3 { Text("Secure storage") }
            val fieldKey = remember { mutableStateOf("") }
            val fieldValue = remember { mutableStateOf("") }
            val message = remember { mutableStateOf("") }
            val restorableState = remember { mutableStateOf(false) }
            Div {
                Text("Start type title of key. If value will be found in device storage, it will be shown in value input")
            }

            Input(InputType.Text) {
                placeholder("Key")
                value(fieldKey.value)
                onInput {
                    fieldKey.value = it.value
                    webApp.secureStorage.getItem(it.value) { e, v, restorable ->
                        fieldValue.value = v ?: ""
                        restorableState.value = restorable == true
                        if (v == null) {
                            if (restorable == true) {
                                message.value = "Value for key \"${it.value}\" has not been found, but can be restored"
                            } else {
                                message.value = "Value for key \"${it.value}\" has not been found. Error: $e"
                            }
                        } else {
                            message.value = "Value for key \"${it.value}\" has been found: \"$v\""
                        }
                    }
                }
            }
            if (restorableState.value) {
                Button({
                    onClick {
                        webApp.secureStorage.restoreItem(fieldKey.value) { e, v ->
                            fieldValue.value = v ?: ""
                            if (v == null) {
                                message.value = "Value for key \"${fieldKey.value}\" has not been restored. Error: $e"
                            } else {
                                message.value = "Value for key \"${fieldKey.value}\" has been restored: \"$v\""
                            }
                        }
                    }
                }) {
                    Text("Restore")
                }
            }
            Div {
                Text("If you want to change value if typed key - just put it here")
            }
            Input(InputType.Text) {
                placeholder("Value")
                value(fieldValue.value)
                onInput {
                    fieldValue.value = it.value
                    webApp.secureStorage.setItem(fieldKey.value, it.value) { e, v ->
                        if (v) {
                            fieldValue.value = it.value
                            message.value = "Value \"${it.value}\" has been saved"
                        } else {
                            message.value = "Value \"${it.value}\" has not been saved. Error: $e"
                        }
                    }
                }
            }

            if (message.value.isNotEmpty()) {
                Div { Text(message.value) }
            }
        }
        P()

        H3 { Text("Events") }
        EventType.values().forEach { eventType ->
            when (eventType) {
                EventType.AccelerometerChanged -> webApp.onAccelerometerChanged { /*logsState.add("AccelerometerChanged") /* see accelerometer block */ */ }
                EventType.AccelerometerFailed -> webApp.onAccelerometerFailed {
                    logsState.add(it.error)
                }
                EventType.AccelerometerStarted -> webApp.onAccelerometerStarted { logsState.add("AccelerometerStarted") }
                EventType.AccelerometerStopped -> webApp.onAccelerometerStopped { logsState.add("AccelerometerStopped") }
                EventType.Activated -> webApp.onActivated { logsState.add("Activated") }
                EventType.BackButtonClicked -> webApp.onBackButtonClicked { logsState.add("BackButtonClicked") }
                EventType.BiometricAuthRequested -> webApp.onBiometricAuthRequested {
                    logsState.add(it.isAuthenticated)
                }
                EventType.BiometricManagerUpdated -> webApp.onBiometricManagerUpdated { logsState.add("BiometricManagerUpdated") }
                EventType.BiometricTokenUpdated -> webApp.onBiometricTokenUpdated {
                    logsState.add(it.isUpdated)
                }
                EventType.ClipboardTextReceived -> webApp.onClipboardTextReceived {
                    logsState.add(it.data)
                }
                EventType.ContactRequested -> webApp.onContactRequested {
                    logsState.add(it.status)
                }
                EventType.ContentSafeAreaChanged -> webApp.onContentSafeAreaChanged { logsState.add("ContentSafeAreaChanged") }
                EventType.Deactivated -> webApp.onDeactivated { logsState.add("Deactivated") }
                EventType.DeviceOrientationChanged -> webApp.onDeviceOrientationChanged { /*logsState.add("DeviceOrientationChanged")*//* see accelerometer block */ }
                EventType.DeviceOrientationFailed -> webApp.onDeviceOrientationFailed {
                    logsState.add(it.error)
                }
                EventType.DeviceOrientationStarted -> webApp.onDeviceOrientationStarted { logsState.add("DeviceOrientationStarted") }
                EventType.DeviceOrientationStopped -> webApp.onDeviceOrientationStopped { logsState.add("DeviceOrientationStopped") }
                EventType.EmojiStatusAccessRequested -> webApp.onEmojiStatusAccessRequested {
                    logsState.add(it.status)
                }
                EventType.EmojiStatusFailed -> webApp.onEmojiStatusFailed {
                    logsState.add(it.error)
                }
                EventType.EmojiStatusSet -> webApp.onEmojiStatusSet { logsState.add("EmojiStatusSet") }
                EventType.FileDownloadRequested -> webApp.onFileDownloadRequested {
                    logsState.add(it.status)
                }
                EventType.FullscreenChanged -> webApp.onFullscreenChanged { logsState.add("FullscreenChanged") }
                EventType.FullscreenFailed -> webApp.onFullscreenFailed {
                    logsState.add(it.error)
                }
                EventType.GyroscopeChanged -> webApp.onGyroscopeChanged { /*logsState.add("GyroscopeChanged")*//* see gyroscope block */ }
                EventType.GyroscopeFailed -> webApp.onGyroscopeFailed {
                    logsState.add(it.error)
                }
                EventType.GyroscopeStarted -> webApp.onGyroscopeStarted { logsState.add("GyroscopeStarted")/* see accelerometer block */ }
                EventType.GyroscopeStopped -> webApp.onGyroscopeStopped { logsState.add("GyroscopeStopped") }
                EventType.HomeScreenAdded -> webApp.onHomeScreenAdded { logsState.add("HomeScreenAdded") }
                EventType.HomeScreenChecked -> webApp.onHomeScreenChecked {
                    logsState.add(it.status)
                }
                EventType.InvoiceClosed -> webApp.onInvoiceClosed { url, status ->
                    logsState.add(url)
                    logsState.add(status)
                }
                EventType.LocationManagerUpdated -> webApp.onLocationManagerUpdated { logsState.add("LocationManagerUpdated") }
                EventType.LocationRequested -> webApp.onLocationRequested {
                    logsState.add(it.locationData)
                }
                EventType.MainButtonClicked -> webApp.onMainButtonClicked { logsState.add("MainButtonClicked") }
                EventType.PopupClosed -> webApp.onPopupClosed {
                    logsState.add(it.buttonId)
                }
                EventType.QrTextReceived -> webApp.onQrTextReceived {
                    logsState.add(it.data)
                }
                EventType.SafeAreaChanged -> webApp.onSafeAreaChanged { logsState.add("SafeAreaChanged") }
                EventType.ScanQrPopupClosed -> webApp.onScanQrPopupClosed { logsState.add("ScanQrPopupClosed") }
                EventType.SecondaryButtonClicked -> webApp.onSecondaryButtonClicked { logsState.add("SecondaryButtonClicked") }
                EventType.SettingsButtonClicked -> webApp.onSettingsButtonClicked { logsState.add("SettingsButtonClicked") }
                EventType.ShareMessageFailed -> webApp.onShareMessageFailed {
                    logsState.add(it.error)
                }
                EventType.ShareMessageSent -> webApp.onShareMessageSent { logsState.add("ShareMessageSent") }
                EventType.ThemeChanged -> webApp.onThemeChanged { logsState.add("ThemeChanged") }
                EventType.ViewportChanged -> webApp.onViewportChanged {
                    logsState.add(it)
                }
                EventType.WriteAccessRequested -> webApp.onWriteAccessRequested {
                    logsState.add(it.status)
                }
            }
        }

        logsState.forEach {
            P { Text(it.toString()) }
        }
    }
}
