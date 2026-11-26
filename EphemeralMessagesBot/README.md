# EphemeralMessagesBot

Demonstrates messages that Telegram shows to a single recipient. A button press supplies the destination for
an ephemeral callback reply. For a direct send in a group, the example supplies both the chat and the receiver.

## Behavior

- `/ephemeral` replies with a **Reveal a secret** inline button. The command is registered with Telegram as
  an ephemeral command.
- Pressing **Reveal a secret** sends a separate personal rich message with `sendRichMessage(callbackQueryId = …)`.
  The bot acknowledges the press with `answer(query)`. Its handler accepts callbacks with either a message or an
  inline message ID. It uses the chat returned by the send to edit the secret after three seconds and delete it
  three seconds later.
- `/ephemeral_photo` waits for a photo from the same user and chat, then sends it back by its Telegram file ID
  with `receiverUserId` set to the user's ID. The bot downloads the photo and edits the ephemeral media using a
  new multipart upload. A second edit sets
  `showCaptionAboveMedia = true`.
- `/ephemeral_live_photo` waits for a Live Photo from the same user and chat and sends it ephemerally using existing
  file IDs. When a cover photo is available, it uploads both the main file and the cover in one
  `editEphemeralMessageMedia` request.
- When the bot receives an ephemeral message with an identifiable recipient, it replies twice. The general `reply`
  derives the recipient and ephemeral message ID. The explicit `replyToEphemeral` takes the chat ID, recipient ID,
  and ephemeral message ID.
- Updates and basic bot information are printed to standard output.

## Setup

Add the bot to a group where it can send messages and media. To try either photo command with group privacy mode
enabled, send the photo as a reply to the bot's prompt. The photo demonstrations download the selected media into
memory before uploading it again.

## Run

From the repository root, pass the token as the first application argument:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN"
```

Optional, case-sensitive arguments may follow the token:

- `debug` enables formatted library logging on standard output.
- `testServer` connects the bot to Telegram's test server.
- `inlineCallbacks` calls `setMySettings` so that buttons in bot-sent messages produce callbacks with
  an inline message ID. Run `/ephemeral` and press its button to try this delivery mode. The setting
  persists until you change it. Use `defaultCallbacks` on a later launch to restore Telegram's default.

For example:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN debug testServer"
```

The token is required; starting without it fails before polling begins. Stop the bot with `Ctrl+C`.
