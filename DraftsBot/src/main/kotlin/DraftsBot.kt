import com.benasher44.uuid.uuid4
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.forum.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessageDraftFlow
import dev.inmo.tgbotapi.extensions.api.send.sendMessageDraftFlowWithTexts
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
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
import io.ktor.client.plugins.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

const val testText = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.    
"""

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
        onCommand("test_draft_flow") {
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

        setMyCommands(
            BotCommand("test_draft_flow", "Start draft testing with flow"),
            scope = BotCommandScope.AllGroupChats
        )
        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
