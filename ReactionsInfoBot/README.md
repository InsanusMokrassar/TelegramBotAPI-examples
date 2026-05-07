# ReactionsInfoBot

A bot that tracks message reactions and reports them back to the user.

## Functionality

Monitors reaction updates in the bot's private chat. When a user adds or removes a reaction on a
message, the bot replies to that message with a formatted summary of the current reactions.
The bot also sets a reaction emoji on incoming messages to acknowledge them.

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

- Handles `MessageReactionUpdated` events (per-user reaction changes)
- Handles `MessageReactionCountUpdated` events (aggregate reaction counts)
- Identifies reaction types: standard emoji, custom emoji, paid reactions
- Replies to the reacted-to message with a formatted list of current reactions
- Sets a reaction on received messages using `setMessageReaction`
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
