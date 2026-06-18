# GiftsBot

A bot that retrieves and displays all gifts received by a user, chat, or business account.

## Functionality

On `/start`, the bot fetches gifts from multiple sources (user gifts, chat gifts, business account
gifts) and sends a formatted summary to the user. Each gift is described with its type (regular or
unique, standard or business-owned) and relevant metadata.

## Arguments

| Position | Value | Sample | Description |
|----------|-------|--------|-------------|
| 1 | `BOT_TOKEN` | `1234567890:AABBccDDeeFF` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Fetch and display all gifts for the requesting user |

## Capabilities

- Retrieves user gifts via `getUserGifts`
- Retrieves chat gifts via `getChatGifts`
- Retrieves business account gifts via `getBusinessAccountGifts`
- Distinguishes between regular gifts and unique gifts
- Distinguishes between standard (user-owned) and business-owned gifts
- Paginates through the full gift list
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
