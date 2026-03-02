import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.abstracts.FromUser
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.business.getBusinessAccountGiftsFlow
import dev.inmo.tgbotapi.extensions.api.chat.members.setChatMemberTag
import dev.inmo.tgbotapi.extensions.api.gifts.getChatGiftsFlow
import dev.inmo.tgbotapi.extensions.api.gifts.getUserGiftsFlow
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.withTypingAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCompleted
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayContent
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onGiveawayWinners
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sender_chat
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.sender_tag
import dev.inmo.tgbotapi.extensions.utils.fromUserOrNull
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.idChatIdentifierOrNull
import dev.inmo.tgbotapi.extensions.utils.potentiallyFromUserGroupContentMessageOrNull
import dev.inmo.tgbotapi.types.UserTag
import dev.inmo.tgbotapi.types.chat.BusinessChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.UnknownChatType
import dev.inmo.tgbotapi.types.gifts.OwnedGift
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.textsources.splitForText
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

suspend fun main(vararg args: String) {
    val botToken = args.first()

    val isDebug = args.any { it == "debug" }
    val isTestServer = args.any { it == "testServer" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    telegramBotWithBehaviourAndLongPolling(botToken, testServer = isTestServer) {
        // start here!!
        val me = getMe()
        println(me)

        onCommand("setChatMemberTag", requireOnlyCommandInMessage = false) {
            val reply = it.replyTo ?.groupContentMessageOrNull() ?: return@onCommand
            val title = it.content.text.removePrefix("/setChatMemberTag").removePrefix(" ")
            setChatMemberTag(
                chatId = reply.chat.id,
                userId = reply.fromUserOrNull() ?.user ?.id ?: return@onCommand,
                tag = UserTag(title)
            )
        }

        onCommand("removeChatMemberTag") {
            val reply = it.replyTo ?.groupContentMessageOrNull() ?: return@onCommand
            setChatMemberTag(
                chatId = reply.chat.id,
                userId = reply.fromUserOrNull() ?.user ?.id ?: return@onCommand,
                tag = null
            )
        }

        onContentMessage {
            val groupContentMessage = it.potentiallyFromUserGroupContentMessageOrNull() ?: return@onContentMessage
            reply(it, "Tag after casting: ${groupContentMessage.senderTag}")
            reply(it, "Tag by getting via risk API: ${it.sender_tag}")
        }

//        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
//            println(it)
//        }
    }.second.join()
}
