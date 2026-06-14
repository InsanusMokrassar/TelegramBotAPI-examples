import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.getUserPersonalChatMessages
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.managed_bots.getManagedBotAccessSettings
import dev.inmo.tgbotapi.extensions.api.managed_bots.getManagedBotToken
import dev.inmo.tgbotapi.extensions.api.managed_bots.replaceManagedBotToken
import dev.inmo.tgbotapi.extensions.api.managed_bots.setManagedBotAccessSettings
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onManagedBotCreated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onManagedBotUpdated
import dev.inmo.tgbotapi.extensions.utils.chatEventMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.managedBotCreatedOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatReplyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestManagedBotButton
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.Username
import dev.inmo.tgbotapi.types.buttons.KeyboardButtonRequestManagedBot
import dev.inmo.tgbotapi.types.buttons.PreparedKeyboardButtonId
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private var BehaviourContextData.update: Update?
    get() = get("update") as? Update
    set(value) = set("update", value)

private var BehaviourContextData.commonMessage: ChatContentMessage<*>?
    get() = get("commonMessage") as? ChatContentMessage<*>
    set(value) = set("commonMessage", value)

/**
 * This place can be the playground for your code.
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
        testServer = isTestServer,
        builder = {
            includeMiddlewares {
                addMiddleware {
                    doOnRequestReturnResult { result, request, _ ->
                        println("Result of $request:\n\n$result")
                        null
                    }
                }
            }
        },
        subcontextInitialAction = buildSubcontextInitialAction {
            add {
                data.update = it
            }
        }
    ) {
        // start here!!
        val me = getMe()
        println(me)

        onCommand("start") {
            println(data.update)
            println(data.commonMessage)
            println(getChat(it.chat))
        }

        onCommand("canManageBots") {
            val me = getMe()
            reply(it, if (me.canManageBots) "Yes" else "No")
        }

        val requestId = RequestId(0)
        onCommand("keyboard") {
            reply(
                it,
                "Keyboard",
                replyMarkup = flatReplyKeyboard(
                    resizeKeyboard = true,
                    oneTimeKeyboard = true,
                ) {
                    requestManagedBotButton(
                        "Add managed bot",
                        KeyboardButtonRequestManagedBot(
                            requestId = requestId,
                            suggestedName = "SampleName",
                            suggestedUsername = Username("@some_sample_bot")
                        )
                    )
                }
            )
        }

        onManagedBotCreated {
            val botChatId = it.chatEvent.bot.id.toChatId()
            reply(it, "Managed bot created successfully: ${it.chatEvent.bot}\nBot ID: ${botChatId.chatId.long}")
            val token = getManagedBotToken(botChatId)
            val accessSettings = getManagedBotAccessSettings(botChatId)
            reply(it, "Token: $token; Access settings: $accessSettings")
        }

        onManagedBotUpdated {
            val botChatId = it.bot.id.toChatId()
            send(it.user, "Managed bot has been updated: ${it.bot}\nBot ID: ${botChatId.chatId.long}")
            val token = getManagedBotToken(botChatId)
            val accessSettings = getManagedBotAccessSettings(botChatId)
            send(it.user, "Token: $token; Access settings: $accessSettings")
        }

        onCommand("replaceToken") {
            val reply = it.replyTo ?.chatEventMessageOrNull() ?: return@onCommand
            val managedBotCreated = reply.chatEvent.managedBotCreatedOrNull() ?: return@onCommand

            reply(it, "Token in replace update: ${replaceManagedBotToken(managedBotCreated.bot.id.toChatId())}")
        }

        // getManagedBotAccessSettings — show BotAccessSettings: who can access the given managed bot
        // Usage: /get_bot_access_settings <botId>
        onCommandWithArgs("get_bot_access_settings") { message, args ->
            val botId = args.firstOrNull()?.toLongOrNull()?.let(::RawChatId)?.toChatId()
                ?: run { reply(message, "Usage: /get_bot_access_settings <botId>\n(Bot ID shown after /keyboard → create bot)"); return@onCommandWithArgs }
            val settings = runCatching { getManagedBotAccessSettings(botId) }.getOrElse {
                reply(message, "Error: ${it.message}"); return@onCommandWithArgs
            }
            reply(message, buildString {
                append("Access settings for managed bot $botId:\n")
                append("  isAccessRestricted: ${settings.isAccessRestricted}\n")
                if (settings.addedUsers != null) {
                    append("  allowedUsers: ${settings.addedUsers!!.joinToString { "${it.firstName} (${it.id})" }}")
                } else {
                    append("  allowedUsers: all (unrestricted)")
                }
            })
        }

        // setManagedBotAccessSettings — restrict access to a list of user IDs, or open to all
        // Usage: /set_bot_access_settings <botId> [userId1 userId2 ...]
        // Omit userIds to open access to all users (addedUserIds = null)
        onCommandWithArgs("set_bot_access_settings") { message, args ->
            val botId = args.firstOrNull()?.toLongOrNull()?.let(::RawChatId)?.toChatId()
                ?: run { reply(message, "Usage: /set_bot_access_settings <botId> [userId1 userId2 ...]"); return@onCommandWithArgs }
            val allowedIds = args.drop(1).mapNotNull { it.toLongOrNull()?.let(::RawChatId)?.toChatId() }
            val addedUserIds: List<ChatId>? = allowedIds.ifEmpty { null }
            runCatching {
                setManagedBotAccessSettings(botId, addedUserIds)
            }.onSuccess {
                reply(message, if (addedUserIds == null) "Access opened to all users." else "Access restricted to ${addedUserIds.size} user(s).")
            }.onFailure {
                reply(message, "Error: ${it.message}")
            }
        }

        // getUserPersonalChatMessages — get recent messages from the user's personal channel
        // Works only if the user has a personal channel linked to their account
        onCommand("get_personal_messages") {
            val msg = it
            val userId = msg.chat.id.toChatId()
            val messages = runCatching { getUserPersonalChatMessages(userId, limit = 10) }.getOrElse { e ->
                reply(msg, "Error: ${e.message}"); return@onCommand
            }
            reply(msg, "Personal channel messages (${messages.size}):\n" +
                messages.joinToString("\n") { m -> "  [${m.messageId}] ${m.content::class.simpleName}" }
                    .ifEmpty { "  (none)" }
            )
        }

        // Bot-to-bot communication: send a message to another bot by @username
        // Since TG Bot API 9.0: works if both bots have bot-to-bot communication enabled in BotFather
        onCommandWithArgs("send_to_bot") { message, args ->
            val usernameArg = args.firstOrNull() ?: run { reply(message, "Usage: /send_to_bot @username [text]"); return@onCommandWithArgs }
            val targetUsername = Username.prepare(usernameArg)
            val text = args.drop(1).joinToString(" ").ifEmpty { "Hello from bot-to-bot communication!" }
            runCatching {
                sendMessage(targetUsername, text)
            }.onSuccess {
                reply(message, "Message sent to $targetUsername")
            }.onFailure {
                reply(message, "Failed to send to $targetUsername: ${it.message}")
            }
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
