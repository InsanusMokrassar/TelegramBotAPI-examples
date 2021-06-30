import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.micro_utils.fsm.common.dsl.buildFSM
import dev.inmo.micro_utils.fsm.common.dsl.strictlyOn
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithParams
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.extensions.utils.shortcuts.chat
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

sealed interface State : State
data class ExpectContentOrStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : State
data class StopState(override val context: ChatId) : State

fun TextContent.containsStopCommand() = parseCommandsWithParams().keys.firstOrNull { it == "stop" } != null

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    telegramBotWithBehaviour(botToken, CoroutineScope(Dispatchers.IO)) {
        val fsm = buildFSM {
            strictlyOn<ExpectContentOrStopState> {
                sendMessage(
                    it.context,
                    buildEntities {
                        +"Send me some content or "
                        botCommand("stop")
                        +" if you want to stop sending"
                    }
                )

                val content = oneOf(
                    parallel {
                        waitContentMessage(includeMediaGroups = false) { if (chat.id == it.context) content else null }.also(::println)
                    },
                    parallel {
                        waitMediaGroup { chat ?.id == it.context }.also(::println)
                    },
                    parallel {
                        waitText { if (content.containsStopCommand()) content else null }.also(::println)
                    }
                ).first()

                when {
                    content is TextContent && content.containsStopCommand() -> StopState(it.context) // assume we got "stop" command
                    content is List<*> -> { // assume it is media group
                        val casted = (content as List<MediaGroupContent>)

                        reply(it.sourceMessage, "Ok, I got this media group and now will resend it to you")
                        sendMediaGroup(it.context, casted.map { it.toMediaGroupMemberInputMedia() })

                        it
                    }
                    content is MessageContent -> {

                        reply(it.sourceMessage, "Ok, I got this content and now will resend it to you")
                        execute(content.createResend(it.context))

                        it
                    }
                    else -> {
                        sendMessage(it.context, "Unknown internal error")
                        it
                    }
                }
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
