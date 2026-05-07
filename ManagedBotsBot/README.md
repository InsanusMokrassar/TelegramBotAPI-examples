# ManagedBotsBot

A bot that demonstrates the Managed Bots API: creating child bots and replacing their tokens.

## Functionality

Allows the operator to check whether the bot supports managed bots, create new managed bots via a
keyboard button, and replace an existing managed bot's token. When a managed bot is created its
token is sent back to the operator. The bot also demonstrates custom middleware and subcontext usage.

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
| `/canManageBots` | Check whether this bot has the ability to create managed bots |
| `/keyboard` | Send a reply keyboard with a *Create managed bot* button |
| `/replaceToken` | Replace the token of a managed bot (send as reply to the bot's token message) |

## Capabilities

- Queries bot capabilities via `getMe` extended fields
- Creates a managed child bot via the `BotKeyboardButton` with `RequestBot` type
- Receives the new bot's info in a `BotShared` service message
- Replaces a managed bot's token via `replaceStickerInSet` (token replacement API)
- Handles `ManagedBotUpdated` events for tracking child bot status changes
- Custom request middleware for logging
- Custom `BehaviourContext` subcontext
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
