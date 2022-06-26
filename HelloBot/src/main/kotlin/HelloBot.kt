import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.types.chat.*
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.SupergroupChat
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
@OptIn(PreviewFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        onContentMessage { message ->
            val chat = message.chat
            if (chat is ChannelChat) {
                val answer = "Hi everybody in this channel \"${chat.title}\""
                send(chat, answer, MarkdownV2)
                return@onContentMessage
            }
            val answerText = "Oh, hi, " + when (chat) {
                is User -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                is PrivateChat -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                is SupergroupChat -> (chat.username ?.username ?: getChat(chat).inviteLink) ?.let {
                    chat.title.linkMarkdownV2(it)
                } ?: chat.title
                is GroupChat -> bot.getChat(chat).inviteLink ?.let {
                    chat.title.linkMarkdownV2(it)
                } ?: chat.title
                else -> "Unknown :(".escapeMarkdownV2Common()
            }
            reply(
                message,
                answerText,
                MarkdownV2
            )
        }
        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }.second.join()
}
