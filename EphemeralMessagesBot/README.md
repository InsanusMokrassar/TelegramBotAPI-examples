# EphemeralMessagesBot

Demonstrates ephemeral messages: group messages that Telegram shows only to one receiver.

## Behavior

- `/ephemeral` replies with a **Reveal a secret** inline button. The command is registered with Telegram as
  an ephemeral command.
- Pressing the button (`reveal` callback data) sends a personal message visible only to the user who
  pressed it. After three seconds the bot edits the message, then deletes it three seconds later.
- When the bot receives an ephemeral content message, it sends two ephemeral replies: one through the
  general `reply` API and one through the explicit `replyToEphemeral` API.
- Updates and basic bot information are printed to standard output.

## Setup

Create a bot token, keep it secret, and add the bot to a group. The bot must be allowed to send messages
there; this example does not request or validate group permissions itself. Use a Telegram environment that
supports ephemeral messages.

## Run

From the repository root, pass the token as the first application argument:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN"
```

Optional, case-sensitive arguments may follow the token:

- `debug` enables formatted library logging on standard output.
- `testServer` connects the bot to Telegram's test server.

For example:

```bash
./gradlew :EphemeralMessagesBot:run --args="BOT_TOKEN debug testServer"
```

The token is required; starting without it fails before polling begins. Stop the bot with `Ctrl+C`.
