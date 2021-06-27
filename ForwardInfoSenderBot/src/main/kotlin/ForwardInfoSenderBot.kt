import dev.inmo.micro_utils.coroutines.defaultSafelyExceptionHandler
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.types.ParseMode.MarkdownV2
import dev.inmo.tgbotapi.types.message.*
import kotlinx.coroutines.*

/**
 * This bot will always return message about forwarder. In cases when sent message was not a forward message it will
 * send suitable message
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviour(botToken, CoroutineScope(Dispatchers.IO)) {
        onContentMessage(includeMediaGroups = true) {
            val toAnswer = buildEntities {
                when (val forwardInfo = it.forwardInfo) {
                    null -> +"There is no forward info"
                    is AnonymousForwardInfo -> {
                        regular("Anonymous user which signed as \"") + code(forwardInfo.senderName) + "\""
                    }
                    is UserForwardInfo -> {
                        val user = forwardInfo.from
                        regular("User ") + code(user.id.chatId.toString()) + " (${user.firstName} ${user.lastName}: ${user.username ?.username ?: "Without username"})"
                    }
                    is ForwardFromChannelInfo -> regular("Channel (") + code((forwardInfo.channelChat).title) + ")"
                    is ForwardFromSupergroupInfo -> regular("Supergroup (") + code((forwardInfo.group).title) + ")"
                }
            }
            reply(it, toAnswer)
            coroutineContext.job.invokeOnCompletion { println("completance of onContentMessage") }
        }
        coroutineContext.job.invokeOnCompletion { println("Completed :)") }
    }.second.join()
}
