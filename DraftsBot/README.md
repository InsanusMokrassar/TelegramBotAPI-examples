# DraftsBot

DraftsBot demonstrates streaming a message draft before sending the finished
message. It receives updates through long polling and uses the same built-in
Lorem ipsum text for both examples.

## Commands

- `/test_draft_flow` publishes progressively longer 50-character prefixes every
  500 ms, then sends the complete text as a normal message.
- `/test_empty_draft` first publishes an empty draft, waits 1.5 seconds, streams
  the same prefixes, and then sends the complete text.

The bot advertises both commands in Telegram's command menu for all group chats.
The handlers themselves are not restricted by chat type, so either command can
also be entered manually in a private chat.

## Setup

1. Obtain a bot token and keep it out of source control.
2. Start a private chat with the bot, or add it to a group where you want to run
   the example.
3. In groups, allow the bot to send messages. No administrator rights are
   otherwise required by this example.

The first program argument is required and must be the bot token. Omitting it
causes startup to fail; any later arguments are ignored.

## Run

From the repository root, run:

```bash
./gradlew :DraftsBot:run --args="<BOT_TOKEN>"
```

> **Known issue:** `DraftsBot/build.gradle` currently declares `TopicsHandlingKt` as the main class, while this bot's entry point is `DraftsBotKt`. The `run` task cannot start until that Gradle setting is corrected; it is left unchanged by this documentation-only update.

Every received update is printed to standard output. Unhandled polling errors
are printed with their stack traces, and HTTP request, socket, and connection
timeouts are each configured to 30 seconds.
