import dev.inmo.micro_utils.coroutines.awaitFirst
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCommandMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.containsCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.sameThread
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.firstOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** State hierarchy for a chat-scoped content-resending conversation. */
sealed interface BotState : State

/**
 * Waits for content or a `/stop` command in the thread of [sourceMessage].
 *
 * @property context Chat whose FSM chain owns this state.
 * @property sourceMessage Message that determines the forum topic/thread to observe.
 */
data class ExpectContentOrStopState(override val context: IdChatIdentifier, val sourceMessage: ChatContentMessage<TextContent>) : BotState

/** Terminal state that acknowledges the end of the chain in [context]. */
data class StopState(override val context: IdChatIdentifier) : BotState

/**
 * Starts the FSM-based resender using the bot token in the first command-line argument.
 *
 * The bot runs with long polling until its coroutine is cancelled.
 */
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

            val contentMessage = firstOf(
                {
                    waitCommandMessage("stop").filter { message ->
                        message.sameThread(it.sourceMessage)
                    }.first()
                    null
                },
                {
                    waitAnyContentMessage().filter { message ->
                        message.sameThread(it.sourceMessage)
                    }.filter {
                        containsCommand(
                            "stop",
                            it.withContentOrNull<TextContent>() ?.content ?.textSources ?: return@filter false
                        ) == false
                    }.first()
                }
            ) ?: return@strictlyOn StopState(it.context)

            val content = contentMessage.content

            execute(content.createResend(it.context))
            it
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

        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
