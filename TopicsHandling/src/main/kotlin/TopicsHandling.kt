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
        suspend fun TelegramBot.isPrivateForumsEnabled(): Boolean {
            val me = getMe()
            if (me.hasTopicsEnabled == false) {
                Log.w("private forums are disabled. That means that they will not work in private chats")
            }
            return me.hasTopicsEnabled
        }
        println()
        flushAccumulatedUpdates()
        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
        onCommand("start_test_topics") {
            if (it.chat is PrivateChat && isPrivateForumsEnabled() == false) {
                return@onCommand
            }
            val forumTopic = createForumTopic(
                it.chat,
                "Test",
                ForumTopic.GREEN
            )

            reply(it, "Test topic has been created")

            delay(1000L)
            editForumTopic(
                it.chat.id,
                forumTopic.messageThreadId,
                "Test 01"
            )

            reply(it, "Test topic has changed its name to Test 01")

            if (it.chat.privateChatOrNull() == null) { // For private forums it is prohibited to close or reopen topics
                delay(1000L)
                closeForumTopic(
                    it.chat.id,
                    forumTopic.messageThreadId,
                )

                reply(it, "Test topic has been closed")

                delay(1000L)
                reopenForumTopic(
                    it.chat.id,
                    forumTopic.messageThreadId,
                )

                reply(it, "Test topic has been reopened")
            }

            delay(1000L)
            deleteForumTopic(
                it.chat.id,
                forumTopic.messageThreadId,
            )

            reply(it, "Test topic has been deleted")

            if (it.chat.privateChatOrNull() == null) { // For private forums it is prohibited to close or reopen topics
                delay(1000L)
                hideGeneralForumTopic(
                    it.chat.id,
                )

                reply(it, "General topic has been hidden")

                delay(1000L)
                unhideGeneralForumTopic(
                    it.chat.id
                )

                reply(it, "General topic has been shown")

                delay(1000L)
                runCatchingSafely(
                    { _ ->
                        reopenGeneralForumTopic(
                            it.chat.id
                        )

                        closeGeneralForumTopic(
                            it.chat.id
                        )
                    }
                ) {
                    closeGeneralForumTopic(
                        it.chat.id
                    )
                }

                reply(it, "General topic has been closed")

                delay(1000L)
                reopenGeneralForumTopic(
                    it.chat.id
                )

                reply(it, "General topic has been opened")

                delay(1000L)
                editGeneralForumTopic(
                    it.chat.id,
                    uuid4().toString().take(10)
                )

                reply(it, "General topic has been renamed")

                delay(1000L)
                editGeneralForumTopic(
                    it.chat.id,
                    "Main topic"
                )

                reply(it, "General topic has been renamed")
            }

            delay(1000L)
        }

        onCommand("delete_topic") {
            val chat = it.chat.forumChatOrNull() ?: return@onCommand

            deleteForumTopic(chat, chat.id.threadId ?: return@onCommand)
        }

        onCommand("unpin_all_forum_topic_messages") {
            val chat = it.chat.forumChatOrNull() ?: return@onCommand

            unpinAllForumTopicMessages(chat, chat.id.threadId ?: return@onCommand)
        }

        onForumTopicCreated {
            reply(it, "Topic has been created")
        }
        onPrivateForumTopicCreated {
            reply(it, "Private topic has been created")
        }

        onForumTopicEdited {
            reply(it, "Topic has been edited")
        }
        onPrivateForumTopicEdited {
            reply(it, "Private topic has been edited")
        }

        onForumTopicReopened {
            reply(it, "Topic has been reopened")
        }
        onGeneralForumTopicHidden {
            reply(it, "General topic has been hidden")
        }
        onGeneralForumTopicUnhidden {
            reply(it, "General topic has been unhidden")
        }

        setMyCommands(
            BotCommand("start_test_topics", "start test topics"),
            BotCommand("delete_topic", "delete topic where message have been sent"),
            BotCommand("unpin_all_forum_topic_messages", "delete topic where message have been sent"),
            scope = BotCommandScope.AllGroupChats
        )
        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
