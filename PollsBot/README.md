# PollsBot

A bot that demonstrates creation and management of Telegram polls (anonymous, public, and quiz).

## Functionality

Creates polls on demand and tracks live answer updates. Users can reply to an existing poll message
to add new options or remove the last option. Quiz polls are created with a random correct answer.
Custom emoji stickers in poll options are supported.

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
| `/anonymous` | Create an anonymous poll |
| `/public` | Create a public poll; users can add options by replying |
| `/quiz` | Create a quiz poll with a randomly chosen correct answer |

All three commands accept an optional custom emoji ID as an extra argument to use in poll option text.

## Capabilities

- Mutex-protected in-memory poll registry to safely track concurrent updates
- Live poll answer updates via `onPollUpdated` handler
- Reply-based option management: reply to a poll message with text to add an option, or reply with `/remove` to delete the last option
- Quiz polls: random correct answer selection, answer explanation included
- Custom emoji in poll option text
- Registers all three commands with Telegram (`setMyCommands`)
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
