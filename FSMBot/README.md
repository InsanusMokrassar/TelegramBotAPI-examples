# FSMBot

A demonstration of the Finite State Machine (FSM) pattern provided by the
[MicroUtils](https://github.com/InsanusMokrassar/MicroUtils) library.

## Functionality

Implements a simple two-state FSM. After `/start` is sent, the bot enters
`ExpectContentOrStopState` and re-sends every message it receives back to the user.
This continues until the user sends `/stop`, at which point the FSM transitions to
`StopState` and content forwarding ends.

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
| `/start` | Starts the FSM loop — bot begins echoing content back to the user |
| `/stop` | Ends the FSM loop — bot stops echoing |

## Capabilities

- Two-state FSM: `ExpectContentOrStopState` → `StopState`
- `ExpectContentOrStopState` uses `expectContentOrCommands()` to filter messages
- Erroneous FSM states are caught and handled gracefully
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
