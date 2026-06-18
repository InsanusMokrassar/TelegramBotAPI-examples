# InlineQueriesBot

A multiplatform bot that answers inline queries with paginated article results.

## Functionality

Responds to inline queries by returning a page of article results. Each result includes a
description and a deep-link button. Navigation between pages is handled via the query offset
(next/previous buttons encoded in the result set).

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

None. The bot is driven by inline queries (type `@BotUsername` in any chat).

## Capabilities

- Answers inline queries with `InlineQueryResultArticle` items
- Offset-based pagination: each result page encodes the next-page offset in the answer
- Each result includes a deep-link `InlineKeyboardButton` back to the bot
- Multiplatform module with a shared `commonMain` implementation and a JVM launcher entry point
- Requires *Inline Mode* to be enabled in BotFather settings
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
