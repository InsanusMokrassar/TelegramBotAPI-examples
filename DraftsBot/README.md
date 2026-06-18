# DraftsBot

A bot that demonstrates the message-draft flow API by progressively revealing text to the user.

## Functionality

On `/test_draft_flow`, the bot sends a series of draft text updates to the user, each building
on the previous one, before committing the final message. This illustrates how to use the draft
message API to stream partial content before finalising it.

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
| `/test_draft_flow` | Starts a draft-message flow that progressively reveals text |

## Capabilities

- Uses the `draftFlow` / `sendDraftMessage` API to emit incremental text updates
- Demonstrates the difference between draft (editable intermediate state) and final message
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
