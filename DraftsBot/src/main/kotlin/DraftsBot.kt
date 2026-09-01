import com.benasher44.uuid.uuid4
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.forum.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessageDraftFlow
import dev.inmo.tgbotapi.extensions.api.send.sendMessageDraftFlowWithTexts
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageGenerationStopped
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageGenerationStopped
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onForumTopicClosed
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onForumTopicCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onForumTopicEdited
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onForumTopicReopened
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGeneralForumTopicHidden
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGeneralForumTopicUnhidden
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPrivateForumTopicCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPrivateForumTopicEdited
import dev.inmo.tgbotapi.extensions.utils.forumChatOrNull
import dev.inmo.tgbotapi.extensions.utils.forumContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull
import dev.inmo.tgbotapi.extensions.utils.privateForumChatOrNull
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.flushAccumulatedUpdates
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.ForumTopic
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import dev.inmo.tgbotapi.utils.DraftIdAllocator
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/** Sample text streamed as a draft and then sent as the completed message. */
const val testText = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.    
"""

private val stoppableDraftIds = DraftIdAllocator()

/**
 * Starts DraftsBot with long polling and registers the draft demonstration commands.
 *
 * The first element of [args] must be the bot token; subsequent elements are ignored.
 * This function remains suspended until the polling job completes.
 *
 * @throws NoSuchElementException when no bot token is supplied
 */
suspend fun main(vararg args: String) {
    telegramBotWithBehaviourAndLongPolling(
        args.first(),
        CoroutineScope(Dispatchers.Default),
        defaultExceptionsHandler = {
            it.printStackTrace()
        },
        builder = {
            client = client.config {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30000
                    socketTimeoutMillis = 30000
                    connectTimeoutMillis = 30000
                }
            }
        }
    ) {
        onCommand("test_draft_flow", initialFilter = { it.chat is PrivateChat }) {
            sendMessageDraftFlowWithTexts(
                it.chat.id,
                flow<String> {
                    val step = 50
                    var currentLength = step
                    while (isActive && testText.length > currentLength) {
                        delay(500L)
                        emit(testText.take(currentLength))
                        currentLength += step
                    }
                },
            )
            send(it.chat, testText)
        }

        // sendMessageDraft now accepts empty text (length 0 is valid since TG Bot API 9.0)
        // Useful to show a typing indicator without any text yet
        onCommand("test_empty_draft", initialFilter = { it.chat is PrivateChat }) {
            sendMessageDraftFlowWithTexts(
                it.chat.id,
                flow<String> {
                    emit("") // empty draft — clears / initializes typing indicator with no content
                    delay(1500L)
                    val step = 50
                    var currentLength = step
                    while (isActive && testText.length > currentLength) {
                        delay(500L)
                        emit(testText.take(currentLength))
                        currentLength += step
                    }
                },
            )
            send(it.chat, testText)
        }

        // Bot API 10.3 lets the user stop generation. The expectation is subscribed before streaming starts so it
        // cannot miss a fast stop update; matching both chat and draft ID also avoids consuming another draft's event.
        onCommand("test_stoppable_draft", initialFilter = { it.chat is PrivateChat }) { origin ->
            val draftId = stoppableDraftIds.allocate()
            val stoppedUpdate = async(start = CoroutineStart.UNDISPATCHED) {
                waitMessageGenerationStopped()
                    .filter { it.chat.id == origin.chat.id && it.draftId == draftId }
                    .first()
            }
            try {
                val completed = sendMessageDraftFlowWithTexts(
                    origin.chat.id,
                    flow<String> {
                        val step = 20
                        var currentLength = step
                        while (isActive) {
                            delay(500L)
                            emit(testText.take(currentLength.coerceAtMost(testText.length)))
                            currentLength = (currentLength + step).coerceAtMost(testText.length)
                        }
                    },
                    draftId = draftId,
                    canStop = true,
                    keepOnStop = true,
                )
                if (!completed) {
                    val stopped = withTimeoutOrNull(5_000L) { stoppedUpdate.await() }
                    if (stopped == null) {
                        println("Draft streaming ended without a matching stopped_message_generation update")
                    } else {
                        // A normal message here would immediately remove the kept draft, so report only to stdout.
                        println(
                            "Expectation matched stopped draft ${stopped.draftId.long} in ${stopped.chat.id}; " +
                                "Telegram kept its last revision temporarily."
                        )
                    }
                }
            } finally {
                stoppedUpdate.cancel()
                stoppableDraftIds.free(draftId)
            }
        }

        // The stopped_message_generation update includes the chat, optional topic and stopped draft ID.
        onMessageGenerationStopped { stopped ->
            println(
                "Message generation stopped in ${stopped.chat.id}; " +
                    "thread=${stopped.messageThreadId}, draft=${stopped.draftId}"
            )
        }

        setMyCommands(
            BotCommand("test_draft_flow", "Start draft testing with flow"),
            BotCommand("test_empty_draft", "Draft starting from empty text (TG Bot API 9.0)"),
            BotCommand("test_stoppable_draft", "Stream a draft that the user can stop"),
            scope = BotCommandScope.AllPrivateChats
        )
        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
