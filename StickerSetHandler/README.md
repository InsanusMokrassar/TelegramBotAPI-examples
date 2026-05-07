# StickerSetHandler

A bot that builds and manages a personal sticker set for each user from stickers they send.

## Functionality

When a user sends a sticker, the bot extracts its emoji and adds it to a per-user sticker set
named `<user_id>_by_<bot_username>`. If the set does not yet exist, it is created first. The bot
supports regular, mask, and custom emoji sticker sets, determined by the type of the first sticker
added. Sending `/delete` removes the user's entire sticker set.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Sends a welcome message explaining how to use the bot |
| `/delete` | Deletes the user's personal sticker set created by this bot |

## Capabilities

- Per-user sticker set with a deterministic name based on user ID and bot username
- Automatic sticker set creation on first sticker received
- Supports all sticker set types: regular, mask, custom emoji
- Emoji extraction from incoming stickers
- Sticker added to an existing set via `addStickerToSet`
- Set deletion via `deleteStickerSet`
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
