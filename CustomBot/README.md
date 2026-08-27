# CustomBot

CustomBot is a diagnostics-heavy playground for experimenting with TelegramBotAPI's behaviour
builder. It uses long polling, prints the result of `getMe` at startup, logs every received update,
and prints every Bot API request and result. It is intended as an example to modify, not as a
production bot.

## Commands and updates

| Command or update | Behaviour |
| --- | --- |
| `/start` | Prints the captured update, context data, and `getChat` result. It fetches profile audios for the current private-chat ID in pages of two, replying with one audio or a two-audio playlist for each non-empty page. |
| `/additional_command` | Demonstrates handler-specific subcontext initialization by storing the command message and printing it with the captured update. It sends no reply. |
| `/getMyStarBalance` | Replies with the bot's current Telegram Stars balance. |
| Channel direct-messages configuration changed | Prints the event to standard output. |

The commands take no arguments. Use `/start` in a private chat: the example deliberately uses the
chat ID as the user ID for `getUserProfileAudios`. No administrator rights are needed for the
private-chat commands. The channel event is only observable when Telegram delivers that update for
a channel in which the bot participates.

## Run

Create a bot, obtain its token, and run this command from the repository root:

```bash
./gradlew :CustomBot:run --args="<BOT_TOKEN>"
```

The token must be the first application argument. Two optional, case-sensitive flags may follow in
either order:

- `debug` enables TelegramBotAPI library logs on standard output. The bot's explicit request,
  result, and update logging is active even without this flag.
- `testServer` selects Telegram's Bot API test environment instead of production.

For example:

```bash
./gradlew :CustomBot:run --args="<BOT_TOKEN> debug testServer"
```

## API concepts demonstrated

- Global and handler-specific `BehaviourContextData` initialization.
- Request/result middleware and subscription to `allUpdatesFlow`.
- Paginated `getUserProfileAudios` calls and audio/media-group replies.
- Command and channel-direct-message event handlers.
