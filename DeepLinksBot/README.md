# DeepLinksBot

An example long-polling bot that creates deep links to itself and demonstrates two
ways to consume their payloads with the TelegramBotAPI behaviour builder.

## Behavior

- On startup, the bot fetches and prints its own account details. It stops if the
  account has no username, because a username is required to build a deep link.
- `/start` without arguments returns a short usage hint.
- A text message containing no bot-command entity is used as the payload of a new
  deep link, which the bot returns to the sender.
- A `/start <payload>` deep link is observed by both an `onDeepLink` trigger and a
  `waitDeepLinks` waiter. Their replies identify which API received the payload.
- The waiter also prints the registered command handlers for demonstration and
  debugging purposes.

Messages containing a bot command are excluded from link generation. The bot has
no persistence and runs until the process is stopped.

## Requirements

- A Telegram bot token supplied as the first command-line argument.
- A username configured for the bot account.
- No special administrator permissions; the bot only needs to receive messages
  and send replies in the chat where it is used.

## Run

From the repository root:

```bash
./gradlew :DeepLinksBot:run --args="BOT_TOKEN"
```

Replace `BOT_TOKEN` with the token for the bot. Additional command-line arguments
are ignored. Omitting the token causes startup to fail before polling begins.
