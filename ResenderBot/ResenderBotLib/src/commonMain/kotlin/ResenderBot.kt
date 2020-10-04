import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.safely
import dev.inmo.tgbotapi.extensions.utils.shortcuts.*
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.types.message.content.abstracts.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.abstracts.MessageContent
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
