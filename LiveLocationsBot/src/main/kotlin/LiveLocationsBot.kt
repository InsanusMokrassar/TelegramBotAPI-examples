import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.EditLiveLocationInfo
import dev.inmo.tgbotapi.extensions.api.edit.location.live.stopLiveLocation
import dev.inmo.tgbotapi.extensions.api.handleLiveLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.LocationContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * This bot will send you live location and update it from time to time
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val locationsFlow = flow {
            var i = 0
            while (isActive) {
                val newInfo = EditLiveLocationInfo(
                    latitude = i.toDouble(),
                    longitude = i.toDouble(),
                    replyMarkup = flatInlineKeyboard {
                        dataButton("Cancel", "cancel")
                    }
                )
                emit(newInfo)
                i++
                delay(3000L) // 3 seconds
            }
        }
        onCommand("start") {
            // in this flow will be actual message with live location
            val currentMessageState = MutableStateFlow<ContentMessage<LocationContent>?>(null)
            val sendingJob = launch {
                handleLiveLocation(
                    it.chat.id,
                    locationsFlow,
                    sentMessageFlow = { currentMessageState.emit(it) },
                )
            }

            waitMessageDataCallbackQuery().filter {
                it.message.sameMessage(
                    currentMessageState.value ?: return@filter false
                ) && it.data == "cancel"
            }.first()

            sendingJob.cancel() // ends live location
            currentMessageState.value ?.let {
                stopLiveLocation(it, replyMarkup = null)
            }
        }
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}

