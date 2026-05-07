# GiveawaysBot

A bot that monitors and logs all giveaway lifecycle events in chats.

## Functionality

Listens for Telegram giveaway service messages and logs each event to standard output. No
interactive commands are provided; the bot is purely an observer/logger for giveaway activity.

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

- Detects and logs giveaway creation events (`GiveawayCreated`)
- Detects and logs giveaway completion events with results (`Giveaway` with results)
- Detects and logs winner announcement messages (`GiveawayWinners`)
- Detects content messages that contain giveaway information (`GiveawayPublicResults`)
- All events are printed to stdout for inspection
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
