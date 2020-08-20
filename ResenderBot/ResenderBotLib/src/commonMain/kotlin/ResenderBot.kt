import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.bot.getMe
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.media.sendMediaGroup
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.safely
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.shortcuts.*
import com.github.insanusmokrassar.TelegramBotAPI.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MediaGroupContent
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.abstracts.MessageContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

suspend fun activateResenderBot(
    token: String,
    print: (Any) -> Unit
) {
    val bot = telegramBot(token)

    print(bot.getMe())

    supervisorScope {
        val scope = this
        bot.startGettingFlowsUpdatesByLongPolling {
            filterContentMessages<MessageContent>(scope).onEach {
                it.content.createResends(it.chat.id, replyToMessageId = it.messageId).forEach {
                    bot.executeUnsafe(it) ?.also {
                        print(it)
                    }
                }
            }.launchIn(scope)
            filterMediaGroupMessages<MediaGroupContent>(scope).onEach {
                safely {
                    bot.sendMediaGroup(
                        it.first().chat,
                        it.map { it.content.toMediaGroupMemberInputMedia() },
                        replyToMessageId = it.first().messageId
                    ).also {
                        print(it)
                    }
                }
            }.launchIn(scope)
        }
    }
}
