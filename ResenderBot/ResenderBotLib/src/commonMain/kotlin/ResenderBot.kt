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
                    bot.executeUnsafe(it) {
                        it.forEach(print)
                    } ?.also {
                        print(it)
                    }
                }
            }.launchIn(scope)
            mediaGroupMessages(scope).onEach {
                safely({ print(it.stackTraceToString()) }) {
                    println(it.chat)
                    bot.execute(it.createResend(it.chat ?: return@safely, replyTo = it.first().messageId)).also {
                        print(it)
                    }
                }
            }.launchIn(scope)
        }
    }
}
