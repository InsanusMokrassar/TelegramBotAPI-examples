# EphemeralMessagesBot

Demonstrates Ephemeral Messages support introduced in Telegram Bot API 10.2.

Add the bot to a group and send `/ephemeral`. Tapping the button makes the bot send a message that only
you can see (an ephemeral message), then edit and delete it. The bot also detects incoming ephemeral
messages and replies to them ephemerally.

## Launch

```bash
../gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN"
```
