import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.getMyStarBalance
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildSubcontextInitialAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onChannelDirectMessagesConfigurationChanged
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
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

        onCommand(
            "additional_command",
            additionalSubcontextInitialAction = { update, commonMessage ->
                data.commonMessage = commonMessage
            }
        ) {
            println(data.update)
            println(data.commonMessage)
        }

        onCommand("getMyStarBalance") {
            reply(
                to = it,
                text = getMyStarBalance().toString()
            )
        }

        onChannelDirectMessagesConfigurationChanged {
            println(it.chatEvent)
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
