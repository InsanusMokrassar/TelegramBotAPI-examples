# DraftsBot

DraftsBot demonstrates streaming a message draft before sending the finished
message. It receives updates through long polling and uses the same built-in
Lorem ipsum text for all examples. Bot API 10.3's stoppable generation controls
and `stopped_message_generation` updates are included.

## Commands

- `/test_draft_flow` publishes progressively longer 50-character prefixes every
  500 ms, then sends the complete text as a normal message.
- `/test_empty_draft` first publishes an empty draft, waits 1.5 seconds, streams
  the same prefixes, and then sends the complete text.
- `/test_stoppable_draft` continuously streams progressively longer revisions
  with `canStop = true` and `keepOnStop = true`. Telegram displays a stop control;
  stopping keeps the most recent draft revision and causes the flow helper to
  return `false`. Before streaming, the handler subscribes to
  `waitMessageGenerationStopped` and filters the expectation by chat and draft
  ID, then logs the matched stop event. It deliberately sends no confirmation
  message, because sending one would immediately remove the draft retained by
  `keepOnStop`. If streaming ends because of another request failure and no
  matching update arrives within five seconds, the bot logs that distinction
  instead of waiting forever.

The bot advertises all three commands in Telegram's command menu for private
chats and filters each handler to private chats, as required by Telegram's draft
methods.

The typed `onMessageGenerationStopped` handler logs the chat, optional topic ID,
and draft ID from every generation-stopped update, independently of the scoped
expectation used by `/test_stoppable_draft`. Both apply to text and rich-message
drafts sent by this bot token.

## Setup

1. Obtain a bot token and keep it out of source control.
2. Start a private chat with the bot. Telegram doesn't accept a group or channel
   ID for `sendMessageDraft`.
3. No administrator rights are required by this example.

The first program argument is required and must be the bot token. Omitting it
causes startup to fail; any later arguments are ignored.

## Run

From the repository root, run:

```bash
./gradlew :DraftsBot:run --args="<BOT_TOKEN>"
```

Every received update is printed to standard output. Unhandled polling errors
are printed with their stack traces, and HTTP request, socket, and connection
timeouts are each configured to 30 seconds.
