# LivePhotosBot

This long-polling example demonstrates receiving, sending, grouping, editing, and selling Telegram Live Photos. It also shows how a regular photo and video from one album can be reused as the two parts of a Live Photo.

## Behavior, commands, and triggers

The bot defines no commands. It prints every incoming update to standard output in addition to the trigger-specific output below.

| Trigger | Behavior |
| --- | --- |
| Standalone Live Photo | Logs its file identifiers, dimensions, duration, thumbnail, MIME type, size, and caption. It resends the Live Photo by file ID, downloads both components, edits the resent message with new multipart files, and uploads the files again as paid content costing 1 Star. |
| Live Photo gallery | Logs every item, downloads each main and cover file, and re-uploads the gallery with `sendMediaGroup`. |
| Paid-media message containing Live Photos | Logs each Live Photo and replies with the number found. Paid-media messages without a Live Photo get no reply from this handler. |
| Edited Live Photo | Logs the file ID and updated caption. |
| Media group containing at least one regular photo and one regular video | Uses the first photo as the cover and the first video as the motion part, then replies with a Live Photo. Albums missing either type are ignored by this handler. |

## Live Photo handling

The initial `sendLivePhoto` call reuses Telegram file IDs. The regular-media edit,
paid-media send, and gallery resend then demonstrate ktgbotapi 37.0.0 collecting
the secondary Live Photo `photo` attachment as well as the main multipart file.
If Telegram supplies no cover photo, the code falls back to the main file ID for
the `photo` field. Downloaded files are held in memory and are not transformed.

The standalone handler performs its requests in order: resend, download, edit,
then send paid media. The edit can therefore succeed in a private or group chat
before the channel-only paid-media request fails.

## Telegram setup and permissions

1. Create a bot with BotFather and obtain its token.
2. Add the bot to the chat where you want to exercise the example and allow it to send messages and media.
3. In a group, make the bot an administrator or disable Group Privacy Mode so it receives ordinary, non-command media albums and Live Photos.
4. To complete the standalone Live Photo flow, use a channel and grant the bot permission to post there: Telegram restricts `sendPaidMedia` to channel chats. In other chat types, the initial resend may succeed but the paid-media request can fail before the edit runs.

The program does not request or validate permissions itself. API errors use the library's normal handling.

## Arguments

The first argument is the required bot token. Optional flags are exact and case-sensitive, can follow the token in either order, and unknown extra arguments are ignored.

| Argument | Effect |
| --- | --- |
| `<BOT_TOKEN>` | Bot token. Omitting it fails before polling starts. |
| `debug` | Prints formatted KSLog diagnostics to standard output. |
| `testServer` | Uses Telegram's Bot API test environment. |

## Launch

From the repository root:

```bash
./gradlew :LivePhotosBot:run --args="<BOT_TOKEN>"
```

For example, to enable both optional flags:

```bash
./gradlew :LivePhotosBot:run --args="<BOT_TOKEN> debug testServer"
```
