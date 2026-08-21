import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.setDefaultKSLog
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitBotSubscriptionUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBotSubscriptionUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.payments.BotSubscriptionUpdated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * Runs a long-polling demonstration of bot payment-subscription updates introduced in Telegram Bot API 10.2.
 *
 * Telegram sends a `subscription` update carrying a [BotSubscriptionUpdated] when a user cancels a recurring
 * payment subscription to the bot, re-enables a canceled subscription, or a subscription payment fails. This
 * example consumes those updates; it does not create recurring invoices.
 *
 * Key concepts demonstrated:
 * - [onBotSubscriptionUpdated] — trigger whose handler receives a [BotSubscriptionUpdated] (`user`,
 *   `invoicePayload`, `state`) and makes a best-effort status notification to the subscriber
 * - [BotSubscriptionUpdated.State] — the typed sealed state: [BotSubscriptionUpdated.State.Active],
 *   [BotSubscriptionUpdated.State.Canceled], [BotSubscriptionUpdated.State.Failed] (data objects) and the
 *   [BotSubscriptionUpdated.State.Unknown] value-class fallback for any future state
 * - `botSubscriptionUpdatedUpdatesFlow` — the raw update flow of
 *   [dev.inmo.tgbotapi.types.update.BotSubscriptionUpdatedUpdate] (available directly because a
 *   BehaviourContext is a `FlowsUpdatesFilter`); each emission's payload is its `data`
 * - [waitBotSubscriptionUpdated] — expectation returning a flow of [BotSubscriptionUpdated]; the
 *   `/wait_subscription` handler takes its next value without a timeout and replies in the command's chat
 *
 * The first command-line argument is always treated as the bot token. Later arguments equal to `debug` and
 * `testServer` enable console diagnostic logging and Telegram's Bot API test environment, respectively.
 *
 * @param args bot token followed by optional, case-sensitive `debug` and `testServer` flags
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
    ) {
        val me = getMe()
        println("Bot info: $me")

        // subscription update: react to the typed BotSubscriptionUpdated.State
        onBotSubscriptionUpdated { update ->
            val user = update.user
            val payload = update.invoicePayload
            val stateText = when (val state = update.state) {
                BotSubscriptionUpdated.State.Active -> "active ✅"
                BotSubscriptionUpdated.State.Canceled -> "canceled ❌"
                BotSubscriptionUpdated.State.Failed -> "payment failed ⚠️"
                // Unknown is a value class carrying the raw state name — future-proof fallback
                is BotSubscriptionUpdated.State.Unknown -> "unknown (${state.name})"
            }
            println("Subscription update from ${user.id}: payload=$payload, state=${update.state.name}")

            // notify the subscriber (only works if they have an open chat with the bot)
            runCatchingLogging {
                send(user.id, "Your subscription (payload: $payload) is now: $stateText")
            }
        }

        // Raw flow variant of the same updates. BehaviourContext : FlowsUpdatesFilter, so the flow is
        // available directly; each emission is a BotSubscriptionUpdatedUpdate whose payload is `.data`.
        botSubscriptionUpdatedUpdatesFlow.subscribeSafelyWithoutExceptions(this) { update ->
            println("[flow] update ${update.updateId}: user=${update.data.user.id}, state=${update.data.state.name}")
        }

        // waitBotSubscriptionUpdated expectation: suspend until the next subscription update
        onCommand("wait_subscription") {
            reply(it, "Waiting for the next subscription update...")
            val update = waitBotSubscriptionUpdated().first()
            reply(it, "Subscription update: state=${update.state.name}, payload=${update.invoicePayload}")
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
