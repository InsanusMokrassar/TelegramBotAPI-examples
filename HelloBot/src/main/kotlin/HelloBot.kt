import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.formatting.linkMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.textMentionMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.safely
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.types.ParseMode.MarkdownV2
import dev.inmo.tgbotapi.types.User
import dev.inmo.tgbotapi.types.chat.abstracts.*
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The main purpose of this bot is just to answer "Oh, hi, " and add user mention here
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    scope.launch {
        println(bot.getChatAdministrators((-1001433262056L).toChatId()))
    }

    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        messageFlow.onEach {
            safely {
                val message = it.data
                val chat = message.chat
                val answerText = "Oh, hi, " + when (chat) {
                    is PrivateChat -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is User -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is SupergroupChat -> (chat.username ?.username ?: bot.getChat(chat).inviteLink) ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    is GroupChat -> bot.getChat(chat).inviteLink ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    else -> "Unknown :(".escapeMarkdownV2Common()
                }
                bot.reply(
                    message,
                    answerText,
                    MarkdownV2
                )
            }
        }.launchIn(scope)
        channelPostFlow.onEach {
            safely {
                val chat = it.data.chat
                val message = "Hi everybody in this channel \"${(chat as ChannelChat).title}\""
                bot.sendTextMessage(chat, message, MarkdownV2)
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}