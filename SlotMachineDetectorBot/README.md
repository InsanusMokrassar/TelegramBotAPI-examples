# SlotMachineDetectorBot

A bot that detects slot-machine dice rolls and reports the result.

## Functionality

Listens for dice messages of the *SlotMachine* type. When one is received, it calculates the
combination shown on the three reels and replies with the formatted result.

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

- Filters incoming dice messages specifically for the slot-machine emoji type
- Decodes the numeric dice value into the three reel symbols
- Replies with a human-readable description of the result
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
