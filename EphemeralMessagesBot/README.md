# EphemeralMessagesBot

Demonstrates Telegram Bot API 10.2 and 10.3 ephemeral messages: group messages that Telegram shows only to one receiver.

## Behavior

- `/ephemeral` replies with a **Reveal a secret** inline button. The command is registered with Telegram as
  an ephemeral command.
- Pressing the button (`reveal` callback data) uses `EphemeralMessageParameters` to replace the callback-query
  message with a personal rich message visible only to the user who pressed it. After three seconds the bot replaces
  its text with typed rich blocks, then deletes it three seconds later.
- `/ephemeral_photo` waits for a photo from the same user and chat, sends it back ephemerally by its Telegram file ID,
  downloads it, and edits the ephemeral media using a new multipart upload. A second edit sets
  `showCaptionAboveMedia = true`.
- `/ephemeral_live_photo` waits for a Live Photo, sends it ephemerally by existing file IDs, then downloads and
  re-uploads both its main file and secondary `photo` file in one `editEphemeralMessageMedia` request. This exercises
  ktgbotapi 37.0.0's secondary multipart attachment collection.
- When the bot receives an ephemeral content message, it sends two ephemeral replies: one through the
  general `reply` API and one through the explicit `replyToEphemeral` API. The explicit form also uses
  `EphemeralMessageParameters`.
- Updates and basic bot information are printed to standard output.

## Setup

Create a bot token, keep it secret, and add the bot to a group. The bot must be allowed to send messages
there; this example does not request or validate group permissions itself. Use a Telegram environment that
supports ephemeral messages. The photo demonstrations download all selected media into memory before uploading it
again.

## Run

From the repository root, pass the token as the first application argument:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN"
```

Optional, case-sensitive arguments may follow the token:

- `debug` enables formatted library logging on standard output.
- `testServer` connects the bot to Telegram's test server.

For example:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN debug testServer"
```

The token is required; starting without it fails before polling begins. Stop the bot with `Ctrl+C`.
