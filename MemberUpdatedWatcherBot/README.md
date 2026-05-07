# MemberUpdatedWatcherBot

A bot that monitors all `ChatMemberUpdated` events and sends descriptive notifications to the chat.

## Functionality

Watches for every member status change in all chats the bot is a member of: bot additions,
admin promotions and demotions, user joins and leaves, and permission restriction changes.
For each event the bot sends a human-readable message describing what changed.

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

None.

## Capabilities

- Detects when the bot itself is added to or removed from a chat and sends a greeting/farewell
- Detects when the bot is promoted to or demoted from administrator
- Detects when any user joins or leaves the chat
- Detects when any user is promoted to or demoted from administrator
- Detects granular permission changes (e.g., restrictions added or lifted)
- Uses the `ChatMemberUpdated` extension functions introduced in TelegramBotAPI 18.0.0
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
