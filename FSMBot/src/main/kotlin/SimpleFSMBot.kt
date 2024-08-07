import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.sameThread
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.botCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

sealed interface BotState : State
data class ExpectContentOrStopState(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) : BotState
data class StopState(override val context: IdChatIdentifier) : BotState

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
            when (state) {
                is ExpectContentOrStopState -> {
                    println("Thrown error on ExpectContentOrStopState")
                }
                is StopState -> {
                    println("Thrown error on StopState")
                }
            }
            e.printStackTrace()
            state
        }
    ) {
        strictlyOn<ExpectContentOrStopState> {
            send(
                it.context,
            ) {
                +"Send me some content or " + botCommand("stop") + " if you want to stop sending"
            }

            val contentMessage = waitAnyContentMessage().filter { message ->
                message.sameThread(it.sourceMessage)
            }.first()
            val content = contentMessage.content

            when {
                content is TextContent && content.text == "/stop"
                || content is TextContent && content.parseCommandsWithArgs().keys.contains("stop") -> StopState(it.context)
                else -> {
                    execute(content.createResend(it.context))
                    it
                }
            }
        }
        strictlyOn<StopState> {
            send(it.context) { +"You have stopped sending of content" }

            null
        }

        command(
            "start"
        ) {
            startChain(ExpectContentOrStopState(it.chat.id, it))
        }

        onContentMessage(
            {
                it.content.textContentOrNull() ?.text == "/start"
            }
        ) {
            startChain(ExpectContentOrStopState(it.chat.id, it.withContentOrNull() ?: return@onContentMessage))
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
