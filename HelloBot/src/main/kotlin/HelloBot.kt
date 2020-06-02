import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.chat.get.getChat
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.sendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.formatting.linkMarkdownV2
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.formatting.textMentionMarkdownV2
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownV2
import com.github.insanusmokrassar.TelegramBotAPI.types.User
import com.github.insanusmokrassar.TelegramBotAPI.types.chat.abstracts.*
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.escapeMarkdownV2Common
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

suspend fun main(vararg args: String) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        messageFlow.onEach {
            safely {
                val chat = it.data.chat
                val message = "Oh, hi, " + when (chat) {
                    is PrivateChat -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is User -> "${chat.firstName} ${chat.lastName}".textMentionMarkdownV2(chat.id)
                    is SupergroupChat -> (chat.username ?.username ?: bot.getChat(chat).inviteLink) ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    is GroupChat -> bot.getChat(chat).inviteLink ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    is ChannelChat -> (chat.username ?.username ?: bot.getChat(chat).inviteLink) ?.let {
                        chat.title.linkMarkdownV2(it)
                    } ?: chat.title
                    else -> "Unknown :(".escapeMarkdownV2Common()
                }
                bot.sendTextMessage(chat, message, MarkdownV2)
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}