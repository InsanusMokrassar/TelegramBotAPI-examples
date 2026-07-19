# CommunitiesBot

Demonstrates Communities support introduced in Telegram Bot API 10.2.

Add the bot to a chat that belongs to a community. It reports when the chat is added to or removed from a
community, and `/community` prints the chat's current community (read from `getChat().community`).

## Launch

```bash
../gradlew :CommunitiesBot:run --args="BOT_TOKEN"
```
