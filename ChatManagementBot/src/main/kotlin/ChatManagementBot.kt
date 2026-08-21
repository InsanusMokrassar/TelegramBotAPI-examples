import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChatAdministrators
import dev.inmo.tgbotapi.extensions.api.chat.members.getChatMember
import dev.inmo.tgbotapi.extensions.api.send.deleteAllUserMessageReactions
import dev.inmo.tgbotapi.extensions.api.send.deleteUserMessageReaction
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.chatMemberGotRestrictedFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.chatMemberGotRestrictionsChangedFilter
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.plus
import dev.inmo.tgbotapi.extensions.utils.fromUserChatMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.fromUserMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.publicChatOrNull
import dev.inmo.tgbotapi.extensions.utils.requireRestrictedChatMember
import dev.inmo.tgbotapi.extensions.utils.restrictedMemberChatMemberOrNull
import dev.inmo.tgbotapi.extensions.utils.specialRightsChatMemberOrNull
import dev.inmo.tgbotapi.types.chat.CommonBot
import dev.inmo.tgbotapi.types.chat.ChatPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Runs a long-polling demonstration of the chat-management features introduced in Telegram Bot API 10.0.
 *
 * The bot logs changes to a restricted member's `canReactToMessages` permission, exposes commands for querying
 * member rights and administrators, removes a user's reactions, and logs content messages received from other
 * bots. Reaction deletion requires the bot's `can_delete_messages` administrator right. Receiving unrestricted
 * bot-authored group messages additionally requires Bot-to-Bot Communication Mode, administrator status, and
 * disabled Group Privacy Mode; `canReadAllGroupMessages` only reports the last of those settings.
 *
 * @param args the bot token followed by optional, case-sensitive `debug` and `testServer` flags. `debug` routes
 * library logs to standard output; `testServer` selects Telegram's Bot API test environment.
 */
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

    telegramBotWithBehaviourAndLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        testServer = isTestServer
    ) {
        val me = getMe()
        println("Bot: ${me.firstName} (@${me.username?.username})")

        // canReadAllGroupMessages (can_read_all_group_messages) reports whether Group Privacy Mode is disabled.
        // Bot-to-bot delivery has additional requirements described in the entry-point KDoc and README.
        println("canReadAllGroupMessages: ${me.canReadAllGroupMessages}")

        // Feature 1: can_react_to_messages in ChatMemberRestricted and ChatPermissions
        // RestrictedMemberChatMember implements ChatPermissions, so canReactToMessages
        // appears in both types as required by the Telegram Bot API spec
        onChatMemberUpdated(
            initialFilter = chatMemberGotRestrictedFilter + chatMemberGotRestrictionsChangedFilter
        ) { update ->
            val restricted = update.newChatMemberState.restrictedMemberChatMemberOrNull()
                ?: return@onChatMemberUpdated
            println("Restriction update for ${update.member.firstName}:")
            // canReactToMessages as ChatMemberRestricted field
            println("  canReactToMessages (ChatMemberRestricted): ${restricted.canReactToMessages}")
            // same field via ChatPermissions — RestrictedMemberChatMember : ChatPermissions
            val permissions: ChatPermissions = restricted
            println("  canReactToMessages (ChatPermissions):      ${permissions.canReactToMessages}")
        }

        // Feature 1: can_react_to_messages in ChatMemberRestricted and ChatPermissions
        // RestrictedMemberChatMember implements ChatPermissions, so canReactToMessages
        // appears in both types as required by the Telegram Bot API spec
        onCommand(
            "retrieveRights"
        ) { message ->
            val replyMessage = message.replyTo ?.fromUserChatMessageOrNull() ?: run {
                reply(message) { +"This command works only in groups/supergroups/channels" }
                return@onCommand
            }
            val chatMember = getChatMember(message.chat.id, replyMessage.user.id)
            val chatPermissions = chatMember.restrictedMemberChatMemberOrNull()

            val canReactToMessages = chatPermissions ?.canReactToMessages
            reply(message) { +"Can react to messages: $canReactToMessages" }
        }

        // Feature 2: return_bots parameter in getChatAdministrators
        // retrieveOtherBots = true corresponds to return_bots = true in the Telegram API
        onCommand("admins") { message ->
            val chat = message.chat.publicChatOrNull() ?: run {
                reply(message) { +"This command works only in groups/supergroups/channels" }
                return@onCommand
            }
            val admins = getChatAdministrators(chat, retrieveOtherBots = true)
            reply(message) {
                +"Administrators (retrieveOtherBots=true, includes bots):\n"
                admins.forEach { admin ->
                    val kind = if (admin.user is CommonBot) "bot" else "user"
                    +"• ${admin.user.firstName} [$kind]\n"
                }
            }
        }

        // Feature 4: deleteMessageReaction
        // Deletes a specific reaction by the replied message's author on that message
        onCommand("deleteReaction") { message ->
            val replied = message.replyTo ?.fromUserChatMessageOrNull() ?: run {
                reply(message) { +"Reply to a message to remove that user's reaction from it" }
                return@onCommand
            }
            deleteUserMessageReaction(replied, replied.user.id)
            reply(message) { +"Deleted reaction by ${replied.user.firstName} on the replied message" }
        }

        // Feature 3: deleteAllMessageReactions
        // Deletes up to 10,000 recent reactions that the replied message's author has left in this chat
        onCommand("deleteAllReactions") { message ->
            val replied = message.replyTo?.fromUserMessageOrNull() ?: run {
                reply(message) { +"Reply to a message to clear all reactions of that user in this chat" }
                return@onCommand
            }
            deleteAllUserMessageReactions(message.chat, replied.user.id)
            reply(message) { +"Deleted all reactions by ${replied.user.firstName} in this chat" }
        }

        // Feature 5: messages from other bots in groups
        // This handler logs bot-authored content messages that Telegram delivers to this bot.
        onContentMessage(
            initialFilter = { msg ->
                val user = msg.fromUserMessageOrNull()?.user
                user is CommonBot && user.id != me.id
            }
        ) { message ->
            val sender = message.fromUserMessageOrNull()?.user
            println("Message from other bot received (canReadAllGroupMessages=${me.canReadAllGroupMessages}):")
            println("  sender: ${sender?.firstName} (@${(sender as? CommonBot)?.username?.username})")
            println("  content: ${message.content}")
        }
    }.second.join()
}
