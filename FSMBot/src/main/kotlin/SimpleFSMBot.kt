import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.micro_utils.fsm.common.dsl.buildFSM
import dev.inmo.micro_utils.fsm.common.dsl.strictlyOn
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithParams
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.extensions.utils.shortcuts.chat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import kotlinx.coroutines.*

sealed interface BotState : State
data class ExpectContentOrStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class StopState(override val context: ChatId) : BotState

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    telegramBotWithBehaviour(botToken, CoroutineScope(Dispatchers.IO)) {
        val fsm = buildFSM {
            strictlyOn<ExpectContentOrStopState> {
                sendMessage(
                    it.context,
                    buildEntities {
                        +"Send me some content or " + botCommand("stop") + " if you want to stop sending"
                    }
                )

                doInSubContext(stopOnCompletion = false) {
                    val behaviourSubcontext = this
                    onContentMessage(
                        initialFilter = { message -> message.chat.id == it.context }
                    ) { message ->
                        execute(message.content.createResend(it.context))
                    }
                    onCommand("stop") {
                        behaviourSubcontext.cancel()
                    }
                }.join()

                StopState(it.context)
            }
            strictlyOn<StopState> {
                sendMessage(it.context, "You have stopped sending of content")

                null
            }
        }

        command("start") {
            fsm.startChain(ExpectContentOrStopState(it.chat.id, it))
        }

        fsm.start(this)
    }.second.join()
}
