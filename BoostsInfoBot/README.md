# BoostsInfoBot

A long-polling example bot that shows the boosts a user has added to a channel. It demonstrates Telegram's channel-request reply-keyboard button, the resulting `chat_shared` service message, the `getUserChatBoosts` Bot API method, and `chat_boost` updates.

## Behavior

1. Open a private chat with the bot and send `/start` (the command takes no arguments).
2. The bot replies with a **Click me :)** keyboard button. Pressing it opens Telegram's channel picker. The picker is restricted to channels where the bot is already a member.
3. After a channel is selected, Telegram sends its identifier to the bot in a `chat_shared` service message. The bot accepts only the response associated with its channel-request button (request ID `1`).
4. The bot calls `getUserChatBoosts` for the selected channel and the user who selected it. It replies with each boost's added and expiration dates plus the unformatted boost object.

If that user has no boosts in the channel, the bot says so. If Telegram rejects the request or another error occurs while obtaining the boosts, it replies with `Unable to take info about boosts in shared chat`.

Separately, every `chat_boost` update received while the bot is running is printed as an unformatted object to standard output. These updates represent boosts that were added or changed; removed-boost updates are not handled by this example. This console output is produced whether or not debug logging is enabled.

## Telegram setup and permissions

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- Use `/start` in a private chat. Telegram's request-chat keyboard buttons are available only in private chats.
- Before selecting a channel, add the bot to it and promote it to administrator. The button requires the bot to be a member, but it does not request administrator rights. Telegram requires administrator rights both for `getUserChatBoosts` and for receiving `chat_boost` updates.
- The query returns only boosts added by the user interacting with the bot, not every boost on the selected channel.
- The example uses long polling and automatically deletes any existing webhook for the bot token at startup. Do not run another long-polling consumer for the same token at the same time.

## Launch

From the repository root, pass the bot token as the first application argument:

```bash
./gradlew :BoostsInfoBot:run --args="<BOT_TOKEN>"
```

An optional second argument, exactly `debug`, routes the library's default KSLog output to standard output:

```bash
./gradlew :BoostsInfoBot:run --args="<BOT_TOKEN> debug"
```
