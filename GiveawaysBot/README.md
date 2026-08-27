# GiveawaysBot

A long-polling example that prints giveaway-related Telegram updates to standard output.

## Behavior

At startup, the bot calls `getMe` and prints its own user information. It then prints
updates matched by these TelegramBotAPI handlers:

- `onGiveawayCreated` — a giveaway was created;
- `onGiveawayCompleted` — a giveaway was completed;
- `onGiveawayWinners` — the giveaway winners were published;
- `onGiveawayContent` — a message contains giveaway content.

The bot sends no replies and defines no bot commands.

## Setup and permissions

1. Obtain a bot token and keep it private.
2. Add the bot to every chat whose giveaway updates it should observe, with enough
   access for Telegram to deliver those updates.

The example does not call admin-only methods, store data, or configure a webhook.

## Run

From the repository root:

```bash
./gradlew :GiveawaysBot:run --args="BOT_TOKEN"
```

The first argument is always the required bot token. Optional, case-sensitive flags
may follow it in either order:

- `debug` enables TelegramBotAPI diagnostic logging on standard output;
- `testServer` connects to Telegram's Bot API test environment.

For example:

```bash
./gradlew :GiveawaysBot:run --args="BOT_TOKEN debug testServer"
```
