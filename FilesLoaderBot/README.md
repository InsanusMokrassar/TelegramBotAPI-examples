# FilesLoaderBot

FilesLoaderBot downloads media received through Telegram, stores it on the local
filesystem, and sends the downloaded media back to the same chat. It uses long
polling and logs every received update to standard output.

## Behavior

- `/start` asks the user to send media.
- Any received photo, animation, live photo, video, sticker, document, audio,
  voice message, video note, or supported media group is handled.
- The file is saved under the filename returned by Telegram. On success, the bot
  replies with the absolute saved path and then uploads the media back to the chat.
- Media groups are downloaded to temporary files and returned as a media group.
- Download failures are printed to standard error.

The bot does not restrict users or chats. Run it with a dedicated output directory
and appropriate filesystem quotas if it is exposed beyond trusted users.

## Setup

Create a bot and provide its token as the first command-line argument. The process
must have network access and permission to create and write to the output directory.
The directory is created when absent and defaults to `/tmp/` when omitted.

No Telegram administrator rights are required in a private chat. For group use,
configure the bot so that it receives the media messages you expect it to process.

## Run

From the repository root:

```bash
./gradlew :FilesLoaderBot:run --args='<BOT_TOKEN>'
./gradlew :FilesLoaderBot:run --args='<BOT_TOKEN> /absolute/output/directory'
```

Arguments:

1. `BOT_TOKEN` (required): the Telegram bot token.
2. `OUTPUT_DIRECTORY` (optional): the local destination directory; defaults to
   `/tmp/`.
