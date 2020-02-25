package com.insanusmokrassar.examples

import com.github.insanusmokrassar.TelegramBotAPI.bot.Ktor.KtorRequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.sendTextMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownV2
import com.github.insanusmokrassar.TelegramBotAPI.types.chat.abstracts.ChannelChat
import com.github.insanusmokrassar.TelegramBotAPI.types.message.*
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.PossiblyForwardedMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.update.abstracts.BaseMessageUpdate
import com.github.insanusmokrassar.TelegramBotAPI.utils.*
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.UpdateReceiver
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.startGettingOfUpdates
import kotlinx.coroutines.*

/**
 * This bot will always return message about forwarder. In cases when sent message was not a forward message it will
 * send suitable message
 */
suspend fun main(args: Array<String>) {
    val botToken = args.first()

    val bot = KtorRequestsExecutor(TelegramAPIUrlsKeeper(botToken))

    val scope = CoroutineScope(Dispatchers.Default)

    val callback: UpdateReceiver<BaseMessageUpdate> = { messageUpdate ->
        val message = messageUpdate.data
        val infoToSend = if (message is PossiblyForwardedMessage) {
            val forwardInfo = message.forwardInfo
            when (forwardInfo) {
                null -> "There is no forward info"
                is AnonymousForwardInfo -> "Anonymous user which signed as \"${forwardInfo.senderName.codeMarkdownV2()}\""
                is UserForwardInfo -> forwardInfo.from.let { user ->
                    "User ${user.id.chatId.toString().codeMarkdownV2()} " + "(${user.firstName} ${user.lastName}: ${user.username ?.username ?: "Without username"})".regularMarkdownV2()
                }
                is ForwardFromChannelInfo -> "Channel (".regularMarkdownV2() + (forwardInfo.channelChat as ChannelChat).title.codeMarkdownV2() + ")".regularMarkdownV2()
            }
        } else {
            "There is no forward info"
        }
        bot.sendTextMessage(message.chat, infoToSend, MarkdownV2)
    }

    bot.startGettingOfUpdates(
        messageCallback = callback,
        channelPostCallback = callback,
        scope = scope
    )

    scope.coroutineContext[Job]!!.join()
}
