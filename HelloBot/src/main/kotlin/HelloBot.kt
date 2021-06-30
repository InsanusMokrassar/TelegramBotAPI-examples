import dev.inmo.micro_utils.coroutines.safely
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.asChannelChat
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPolling
import dev.inmo.tgbotapi.types.ParseMode.MarkdownV2
import dev.inmo.tgbotapi.types.User
import dev.inmo.tgbotapi.types.chat.abstracts.*
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviour(botToken, CoroutineScope(Dispatchers.IO)) {
        onContentMessage { message ->
            val chat = message.chat
            if (chat is ChannelChat) {
                val answer = "Hi everybody in this channel \"${chat.title}\""
                sendTextMessage(chat, answer, MarkdownV2)
                return@onContentMessage
            }
            val answerText = "Oh, hi, " + when (chat) {
                is PrivateChat -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                is User -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
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
    }.second.join()
}
