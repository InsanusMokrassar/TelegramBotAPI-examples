import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.chat.forum.*
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.flushAccumulatedUpdates
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.ForumTopic
import dev.inmo.tgbotapi.types.commands.BotCommandScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

suspend fun main(vararg args: String) {
    telegramBotWithBehaviourAndLongPolling(
        args.first(),
        CoroutineScope(Dispatchers.Default),
        defaultExceptionsHandler = {
            it.printStackTrace()
        }
    ) {
        flushAccumulatedUpdates()
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
        onCommand("start_test_topics") {
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

            delay(1000L)
            deleteForumTopic(
                it.chat.id,
                forumTopic.messageThreadId,
            )

            reply(it, "Test topic has been deleted")

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

            delay(1000L)
        }

        setMyCommands(
            BotCommand("start_test_topics", "start test topics"),
            scope = BotCommandScope.AllGroupChats
        )
    }.second.join()
}
