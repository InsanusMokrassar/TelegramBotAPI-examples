# KeyboardsBot

A multiplatform bot (JVM + JS) that demonstrates inline keyboard pagination and various button types.

## Functionality

On `/inline <page> <count>`, the bot sends an inline keyboard built from `count` items starting
at `page`. The keyboard includes previous/next navigation buttons, copy-text buttons, styled
action buttons, and an inline-query chosen-chat button. Callback queries from the buttons navigate
between pages.

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

| Command | Arguments | Description |
|---------|-----------|-------------|
| `/inline` | `<page> <count>` | Send a paginated inline keyboard starting at `page` with `count` items per page |

## Capabilities

- Multi-page inline keyboard navigation (previous / next buttons encoded as callback data)
- Copy-text buttons (`CopyTextButton`)
- Styled action buttons: Primary, Success, Danger colour variants
- Inline query chosen-chat button (`SwitchInlineQueryChosenChat`)
- Answers inline queries that originate from the keyboard buttons
- Shared `commonMain` library with JVM and JS launchers
- Runs via long polling

## Launch

### JVM

```bash
./gradlew :KeyboardsBot:jvm_launcher:run --args="BOT_TOKEN"
```
