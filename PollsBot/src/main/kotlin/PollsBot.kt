import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.api.send.polls.sendRegularPoll
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMentionWithAnyContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollAnswer
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sender_chat
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.ifChannelChat
import dev.inmo.tgbotapi.extensions.utils.ifFromChannelGroupContentMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.PollIdentifier
import dev.inmo.tgbotapi.types.chat.*
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.polls.Poll
import dev.inmo.tgbotapi.types.polls.PollAnswer
import dev.inmo.tgbotapi.types.polls.PollOption
import dev.inmo.tgbotapi.types.polls.RegularPoll
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        val me = getMe()

        val pollToChat = mutableMapOf<PollIdentifier, IdChatIdentifier>()
        val pollToChatMutex = Mutex()

        onCommand("anonymous") {
            val sentPoll = sendRegularPoll(
                it.chat,
                "Test regular anonymous poll",
                (1 .. 10).map {
                    it.toString()
                },
                isAnonymous = true,
                replyToMessageId = it.messageId
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        onCommand("public") {
            val sentPoll = sendRegularPoll(
                it.chat,
                "Test regular anonymous poll",
                (1 .. 10).map {
                    it.toString()
                },
                isAnonymous = false,
                replyToMessageId = it.messageId
            )
            pollToChatMutex.withLock {
                pollToChat[sentPoll.content.poll.id] = sentPoll.chat.id
            }
        }

        onPollAnswer {
            val chatId = pollToChat[it.pollId] ?: return@onPollAnswer

            when(it) {
                is PollAnswer.Public -> send(chatId, "User ${it.user} have answered")
                is PollAnswer.Anonymous -> send(chatId, "Chat ${it.voterChat} have answered")
            }
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
