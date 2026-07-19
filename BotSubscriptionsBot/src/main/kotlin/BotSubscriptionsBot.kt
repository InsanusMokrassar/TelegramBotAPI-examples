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
 * This bot demonstrates Bot Subscriptions (subscription updates) support introduced in Telegram Bot API 10.2.
 *
 * When a user starts, renews, cancels or fails to pay a subscription to the bot (a recurring Telegram Stars
 * payment), the bot receives a `subscription` update carrying a [BotSubscriptionUpdated].
 *
 * Key concepts demonstrated:
 * - [onBotSubscriptionUpdated] — trigger whose handler receives a [BotSubscriptionUpdated] (`user`,
 *   `invoicePayload`, `state`)
 * - [BotSubscriptionUpdated.State] — the typed sealed state: [BotSubscriptionUpdated.State.Active],
 *   [BotSubscriptionUpdated.State.Canceled], [BotSubscriptionUpdated.State.Failed] (data objects) and the
 *   [BotSubscriptionUpdated.State.Unknown] value-class fallback for any future state
 * - `botSubscriptionUpdatedUpdatesFlow` — the raw update flow of
 *   [dev.inmo.tgbotapi.types.update.BotSubscriptionUpdatedUpdate] (available directly because a
 *   BehaviourContext is a `FlowsUpdatesFilter`); each emission's payload is its `data`
 * - [waitBotSubscriptionUpdated] — expectation returning a flow of [BotSubscriptionUpdated]
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
