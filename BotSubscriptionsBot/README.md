# BotSubscriptionsBot

Demonstrates the [`subscription`](https://core.telegram.org/bots/api#update) update added in Telegram Bot API
10.2. Telegram sends a [`BotSubscriptionUpdated`](https://core.telegram.org/bots/api#botsubscriptionupdated) when a
user cancels a recurring payment subscription to the bot, re-enables a canceled subscription, or a subscription
payment fails.

This example only observes subscription changes. It does not create an invoice or start a subscription.

## Behavior

At startup, the bot calls `getMe` and prints its own information. It then receives updates through long polling and
prints every received update object to standard output.

For each subscription update, the bot demonstrates three tgbotapi interfaces:

- `onBotSubscriptionUpdated` handles `BotSubscriptionUpdated` directly. It prints the user ID, invoice payload, and
  typed state, then makes a best-effort attempt to notify that user in a private chat. A send failure is logged and
  does not stop polling.
- `botSubscriptionUpdatedUpdatesFlow` exposes the underlying `BotSubscriptionUpdatedUpdate`; this example prints its
  update ID, user ID, and state. Consequently, the same event appears in the typed-handler, subscription-flow, and
  generic all-update logs.
- `waitBotSubscriptionUpdated().first()` waits for one matching event in the `/wait_subscription` command handler.

The known tgbotapi states are `Active`, `Canceled`, and `Failed`. Unknown state strings are preserved as `Unknown`, so
the example remains compatible if Telegram adds another state.

## Command

- `/wait_subscription` — a standalone command with no arguments. It replies that it is waiting, then waits without a
  timeout for the next subscription update and replies in the command's chat with that update's state and invoice
  payload. The update is not restricted to the command sender, so this unprotected diagnostic command should not be
  copied into a production bot as-is.

There is no `/start` handler and the bot ignores other commands apart from printing their received update objects.

## Setup

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Use a complete payment implementation for that same bot to create a recurring Telegram Stars (`XTR`) invoice link
   with [`createInvoiceLink`](https://core.telegram.org/bots/api#createinvoicelink) and a `subscription_period`
   (currently 2,592,000 seconds, or 30 days), then let a user subscribe. This example has neither an invoice creator nor
   a `pre_checkout_query` handler, so it cannot establish a new subscription by itself; it is intended to observe state
   changes for subscriptions created through that payment flow.
3. Have each subscriber start the bot and leave its private chat unblocked if you want the direct status notification
   to succeed.

No group or channel membership and no administrator permissions are required for bot payment subscriptions. If you
run `/wait_subscription` in a group, the bot only needs to receive the command and be allowed to send its replies.

These events concern recurring payments toward the bot. They are different from paid channel subscription invite
links.

## Arguments

The bot token is required and must be the first argument. The remaining optional flags are exact, case-sensitive
strings and can be supplied in either order:

| Argument | Effect |
| --- | --- |
| `BOT_TOKEN` | Token of the bot to run. |
| `debug` | Sends tgbotapi/KSLog diagnostic output to standard output. |
| `testServer` | Uses Telegram's Bot API test environment (`/test`) instead of the production environment. |

## Launch

From the repository root:

```bash
./gradlew :BotSubscriptionsBot:run --args="BOT_TOKEN"
```

For example, to enable both optional modes:

```bash
./gradlew :BotSubscriptionsBot:run --args="BOT_TOKEN debug testServer"
```
