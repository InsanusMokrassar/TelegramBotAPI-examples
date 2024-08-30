import dev.inmo.kslog.common.*
import dev.inmo.tgbotapi.extensions.api.*
import dev.inmo.tgbotapi.extensions.api.bot.*
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.*
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.chat.member.*
import dev.inmo.tgbotapi.utils.*


@OptIn(PreviewFeature::class)
suspend fun main(args: Array<String>) {
    val token = args.first()

    val isDebug = args.any { it == "debug" }

    if (isDebug) {
        setDefaultKSLog(
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
        )
    }

    val internalLogger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
        println(defaultMessageFormatter(level, tag ?: "ChatMemberUpdates", message, throwable))
    }

    val bot = telegramBot(token)

    bot.buildBehaviourWithLongPolling {
        val me = getMe()
        val filterSelfUpdates = SimpleFilter<ChatMemberUpdated> {
            it.member.id == me.id
        }

        // This bot updates
        onChatMemberJoined(initialFilter = filterSelfUpdates) {
            internalLogger.i("Bot was added to chat")
            sendMessage(it.chat.id, "I was added to chat. Please grant me admin permissions to make me able to watch other users' events")
        }

        onChatMemberGotPromoted(initialFilter = filterSelfUpdates) {
            internalLogger.i("Bot was granted admin permissions")
            sendMessage(it.chat.id, "I was promoted to admin. I now can watch other users' events")
        }

        onChatMemberGotDemoted(initialFilter = filterSelfUpdates) {
            internalLogger.i("Admin permissions were revoked")
            sendMessage(it.chat.id, "I'm no longer an admin. Admin permissions are required to watch other users' events")
        }

        // All users updates
        onChatMemberJoined {
            val member = it.member
            internalLogger.i("${member.firstName} joined the chat: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "Welcome ${member.firstName}")
        }

        onChatMemberLeft {
            val member = it.member
            internalLogger.i("${member.firstName} left the chat: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "Goodbye ${member.firstName}")
        }

        onChatMemberGotPromoted {
            val newState = it.newChatMemberState.administratorChatMemberOrThrow()
            internalLogger.i("${newState.user.firstName} got promoted to ${newState.customTitle ?: "Admin"}: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "${newState.user.firstName} is now an ${newState.customTitle ?: "Admin"}")
        }

        onChatMemberGotDemoted {
            val member = it.member
            internalLogger.i("${member.firstName} got demoted: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
            sendMessage(it.chat.id, "${member.firstName} is now got demoted back to member")
        }

        onChatMemberGotPromotionChanged {
            val member = it.newChatMemberState.administratorChatMemberOrThrow()
            internalLogger.i("${member.user.firstName} has the permissions changed: ${it.oldChatMemberState::class.simpleName} => ${it.newChatMemberState::class.simpleName}")
        }
    }.join()
}