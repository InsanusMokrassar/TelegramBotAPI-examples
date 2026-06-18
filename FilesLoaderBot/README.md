# FilesLoaderBot

A bot that downloads any media file sent to it and then re-uploads it back to the chat.

## Functionality

For every message containing a file (photo, video, audio, document, sticker, animation, voice,
video note, etc.), the bot downloads the file to a local directory, then sends the file back to
the chat. Media groups are expanded and each file is re-sent individually. While processing, the
bot sends an appropriate "upload" chat action (e.g., *uploading video*, *uploading photo*).

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2 *(optional)* | `/path/to/dir` | Directory where files are saved (defaults to `/tmp/`) |

Optional flags (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Sends a usage instruction message |

## Capabilities

- Supports all Telegram media types: photos, videos, audio, documents, stickers, animations, voice messages, video notes
- Handles media groups by iterating over each item and re-uploading it individually
- Sends contextually appropriate chat actions during upload (typing, upload_video, upload_audio, etc.)
- Logs the local file path after each download
- Runs via long polling

## Launch

```bash
# Default directory (/tmp/)
../gradlew run --args="BOT_TOKEN"

# Custom directory
../gradlew run --args="BOT_TOKEN /path/to/save/dir"
```
