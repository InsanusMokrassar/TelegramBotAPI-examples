# StickerInfoBot

A multiplatform bot (JVM + JS) that displays detailed information about stickers and custom emoji.

## Functionality

When the user sends a sticker, the bot replies with the sticker set name, title, and sticker type.
When the user sends a text message containing custom emoji entities, the bot fetches the
corresponding sticker objects and sends back their information.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional flags (any order):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

None.

## Capabilities

- Detects incoming sticker messages and calls `getStickerSet` to retrieve set metadata
- Reports sticker set name, title, and type (regular / mask / custom emoji)
- Scans text message entities for `CustomEmoji` types
- Fetches the corresponding sticker objects via `getCustomEmojiStickers`
- Sends sticker information back as a formatted reply
- Shared `commonMain` library with JVM and JS launchers
- Runs via long polling

## Launch

### JVM

```bash
./gradlew :StickerInfoBot:jvm_launcher:run --args="BOT_TOKEN"
```
