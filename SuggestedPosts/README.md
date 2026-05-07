# SuggestedPosts

A bot that handles the channel Direct Messages (suggested post) approval flow.

## Functionality

Monitors suggested post events in a channel connected via Direct Messages. When a post is
suggested, the bot automatically schedules a decline after a short delay (demonstrating the
decline flow). Paid post events and approval/decline confirmations are also tracked and logged.

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
| `/start` | Initialises the bot and confirms it is running |

## Capabilities

- Handles `SuggestedPostApproved` events
- Handles `SuggestedPostDeclined` events
- Handles `SuggestedPostPaid` and `SuggestedPostRefunded` events
- Handles `SuggestedPostApprovalFailed` errors
- Automatically declines new suggestions after a configurable delay to demonstrate the decline API
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
