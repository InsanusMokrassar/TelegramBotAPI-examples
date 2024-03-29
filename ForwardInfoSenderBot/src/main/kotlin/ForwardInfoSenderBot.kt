import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.formatting.makeLink
import dev.inmo.tgbotapi.types.chat.CommonBot
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.chat.ExtendedBot
import dev.inmo.tgbotapi.types.message.*
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code
import dev.inmo.tgbotapi.utils.link
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.*

/**
 * This bot will always return message about forwarder. In cases when sent message was not a forward message it will
 * send suitable message
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()

    telegramBotWithBehaviourAndLongPolling(botToken, CoroutineScope(Dispatchers.IO)) {
        onContentMessage {
            val toAnswer = buildEntities {
                when (val forwardInfo = it.forwardInfo) {
                    null -> +"There is no forward info"
                    is ForwardInfo.ByAnonymous -> {
                        regular("Anonymous user which signed as \"") + code(forwardInfo.senderName) + "\""
                    }
                    is ForwardInfo.ByUser -> {
                        val user = forwardInfo.from
                        when (user) {
                            is CommonUser -> {
                                if (user.isPremium) {
                                    regular("Premium user ")
                                } else {
                                    regular("User ")
                                }
                            }

                            is CommonBot,
                            is ExtendedBot -> regular("Bot ")
                        } + code(user.id.chatId.toString()) + " (${user.firstName} ${user.lastName}: ${user.username?.username ?: "Without username"})"
                    }
                    is ForwardInfo.PublicChat.FromChannel -> {
                        regular("Channel (") + (forwardInfo.channelChat.username ?.let {
                            link(
                                forwardInfo.channelChat.title,
                                makeLink(it)
                            )
                        } ?: code(forwardInfo.channelChat.title)) + ")"
                    }
                    is ForwardInfo.PublicChat.FromSupergroup -> regular("Supergroup (") + code(forwardInfo.group.title) + ")"
                    is ForwardInfo.PublicChat.SentByChannel -> regular("Sent by channel (") + code(forwardInfo.channelChat.title) + ")"
                }
            }
            reply(it, toAnswer)
        }
    }.second.join()
}
