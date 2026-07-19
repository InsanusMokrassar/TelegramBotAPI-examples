# BotSubscriptionsBot

Demonstrates Bot Subscriptions (subscription updates) support introduced in Telegram Bot API 10.2.

The bot logs and reacts to `subscription` updates — when a user's recurring Telegram Stars subscription to
the bot becomes active, is canceled, or fails — handling the typed `BotSubscriptionUpdated.State`.

## Launch

```bash
../gradlew :BotSubscriptionsBot:run --args="BOT_TOKEN"
```
