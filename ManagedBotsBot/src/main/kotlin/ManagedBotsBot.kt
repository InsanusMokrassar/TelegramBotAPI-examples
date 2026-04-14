import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.managed_bots.getManagedBotToken
import dev.inmo.tgbotapi.extensions.api.managed_bots.replaceManagedBotToken
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onManagedBotCreated
import dev.inmo.tgbotapi.extensions.utils.chatEventMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.managedBotCreatedOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatReplyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.requestManagedBotButton
import dev.inmo.tgbotapi.types.Username
import dev.inmo.tgbotapi.types.buttons.KeyboardButtonRequestManagedBot
import dev.inmo.tgbotapi.types.buttons.PreparedKeyboardButtonId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.request.RequestId
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private var BehaviourContextData.update: Update?
    get() = get("update") as? Update
    set(value) = set("update", value)

private var BehaviourContextData.commonMessage: CommonMessage<*>?
    get() = get("commonMessage") as? CommonMessage<*>
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
            reply(it, "Managed bot created successfully: ${it.chatEvent.bot}")
            val token = getManagedBotToken(
                it.chatEvent.bot.id.toChatId()
            )
            reply(it, "Token: $token")
        }

        onCommand("replaceToken") {
            val reply = it.replyTo ?.chatEventMessageOrNull() ?: return@onCommand
            val managedBotCreated = reply.chatEvent.managedBotCreatedOrNull() ?: return@onCommand

            reply(it, "Token: ${replaceManagedBotToken(managedBotCreated.bot.id.toChatId())}")
        }

        allUpdatesFlow.subscribeLoggingDropExceptions(this) {
            println(it)
        }
    }.second.join()
}
