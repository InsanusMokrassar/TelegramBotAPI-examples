# ManagedBotsBot

A long-polling playground for creating and administering managed bots, inspecting a
user's personal-channel messages, and trying bot-to-bot messages.

## Commands and triggers

- `/start` prints the triggering update, context data, and full chat information;
  it sends no reply.
- `/canManageBots` replies whether `getMe` reports that this bot can manage bots.
- `/keyboard` sends a one-time keyboard for creating a managed bot with suggested
  name `SampleName` and username `@some_sample_bot`.
- `/replaceToken`, when sent as a reply to a managed-bot-created service message,
  replaces that bot's token and replies with the new token.
- `/get_bot_access_settings <botId>` shows whether access is restricted and lists
  allowed users when present.
- `/set_bot_access_settings <botId> [userId ...]` restricts access to the supplied
  numeric user IDs; omitting user IDs opens access to everyone.
- `/get_personal_messages` lists up to ten messages from the current private-chat
  user's linked personal channel.
- `/send_to_bot @username [text]` sends text to another bot; omitted text defaults
  to `Hello from bot-to-bot communication!`.

Managed-bot-created and managed-bot-updated events report the bot and numeric ID,
then retrieve and send its token and access settings. Every update and every API
request result is also printed to standard output. Commands are not registered in
Telegram's command menu.

## Setup, permissions, and safety

1. Create a bot, obtain its token, and verify `/canManageBots` replies `Yes`.
2. Use a private test chat for the managed-bot and personal-channel examples.
3. Link a personal channel before using `/get_personal_messages`.
4. For `/send_to_bot`, enable bot-to-bot communication for both bots in BotFather.

This example exposes managed-bot tokens in chat and logs API results. Use disposable
test bots, keep the chat and process output private, and rotate any exposed token.
No chat-administrator permission is requested by the code.

## Run

The intended command from the repository root is:

```bash
./gradlew :ManagedBotsBot:run --args="BOT_TOKEN"
```

The first argument is the required token. Optional exact flags may follow it in any
order: `debug` enables formatted logging, and `testServer` uses Telegram's test API.

Known issue: `build.gradle` currently sets `mainClassName` to `CustomBotKt`, while
this source's entry point is `ManagedBotsBotKt`; the `run` task cannot start until
that Gradle setting is corrected.
