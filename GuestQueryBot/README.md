# GuestQueryBot

Demonstrates guest queries through long polling in chats where the bot is not a member.

## Behavior

At startup, the bot calls `getMe` and prints its bot information and the value of
`supportsGuestQueries`.

For each guest request, it prints the query ID, caller, chat, and content, then
answers with an inline article whose message contains:

```text
Guest mode reply
Query ID: <guest-query-id>
```

For ordinary content messages carrying guest-call metadata, the bot also replies
with the initiating user's name and/or the public chat's title. Every received
update is printed to standard output. The bot defines no commands.

## Setup and permissions

1. Create a bot and obtain its token; keep the token private.
2. Enable guest queries in BotFather so that `supports_guest_queries` is enabled.
3. For ordinary messages outside guest mode, add the bot to the relevant chat and
   allow it to send messages there.

The guest-query flow does not require the bot to be a chat member. This example
uses no admin-only methods and does not configure a webhook.

## Run

From the repository root:

```bash
./gradlew :GuestQueryBot:run --args="BOT_TOKEN"
```

The first argument is the required bot token. Optional, case-sensitive flags may
follow it in either order:

- `debug` enables formatted library logging on standard output;
- `testServer` connects to Telegram's Bot API test environment.

For example:

```bash
./gradlew :GuestQueryBot:run --args="BOT_TOKEN debug testServer"
```
