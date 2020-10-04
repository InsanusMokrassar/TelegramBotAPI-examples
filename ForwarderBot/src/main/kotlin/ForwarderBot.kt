import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.utils.formatting.codeMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.formatting.regularMarkdownV2
import dev.inmo.tgbotapi.extensions.utils.safely
import dev.inmo.tgbotapi.extensions.utils.updates.asContentMessagesFlow
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingFlowsUpdatesByLongPolling
import dev.inmo.tgbotapi.types.ParseMode.MarkdownV2
import dev.inmo.tgbotapi.types.message.*
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyForwardedMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * This bot will always return message about forwarder. In cases when sent message was not a forward message it will
 * send suitable message
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)

    bot.startGettingFlowsUpdatesByLongPolling(scope = scope) {
        messageFlow.asContentMessagesFlow().mapNotNull { it as? PossiblyForwardedMessage }.onEach { message ->
            safely({ it.printStackTrace() }) {
                val toAnswer = when (val forwardInfo = message.forwardInfo) {
                    null -> "There is no forward info"
                    is AnonymousForwardInfo -> "Anonymous user which signed as \"${forwardInfo.senderName.codeMarkdownV2()}\""
                    is UserForwardInfo -> forwardInfo.from.let { user ->
                        "User ${user.id.chatId.toString().codeMarkdownV2()} " + "(${user.firstName} ${user.lastName}: ${user.username ?.username ?: "Without username"})".regularMarkdownV2()
                    }
                    is ForwardFromChannelInfo -> "Channel (".regularMarkdownV2() + (forwardInfo.channelChat).title.codeMarkdownV2() + ")".regularMarkdownV2()
                    is ForwardFromSupergroupInfo -> "Supergroup (".regularMarkdownV2() + (forwardInfo.group).title.codeMarkdownV2() + ")".regularMarkdownV2()
                }
                bot.sendTextMessage(message.chat, toAnswer, MarkdownV2)
            }
        }.launchIn(scope)
    }

    scope.coroutineContext[Job]!!.join()
}
