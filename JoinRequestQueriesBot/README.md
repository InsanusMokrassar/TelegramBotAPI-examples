# JoinRequestQueriesBot

A long-polling example for processing chat join-request queries as a chat's guard bot.

## Behavior

At startup, the bot calls `getMe` and prints its bot information and
`supportsJoinRequestQueries` value. For every chat join request it prints the
requesting user, chat, bio, query ID, and the chat's configured guard bot.

Only requests containing a query ID are processed. Without a Web App URL, the bot:

- answers with `Queue` when the user's bio is missing or blank, leaving the
  decision to other administrators;
- answers with `Approve` when the user has a nonblank bio.

When an HTTPS Web App URL is supplied, the bot sends that Web App for verification
instead of answering the query. Requests without a query ID are only logged. Every
received update is printed to standard output, and the bot defines no commands.

## Setup and permissions

1. Create a bot, obtain its token, and keep the token private.
2. Configure the bot as the guard bot of the chat whose requests it should handle.
3. Grant it the administrator permission to invite users (`can_invite_users`).
4. If using the Web App flow, provide an HTTPS verification URL.

The example uses long polling and does not configure a webhook. Be aware that its
default flow automatically approves query-backed requests with a nonblank bio.

## Run

From the repository root:

```bash
./gradlew :JoinRequestQueriesBot:run --args="BOT_TOKEN"
```

The first argument is the required bot token. An optional Web App URL is recognized
only as the second argument and must begin with `https://`. The case-sensitive flags
`debug` and `testServer` enable formatted logging and Telegram's test environment;
they may follow the token and Web App URL.

For example:

```bash
./gradlew :JoinRequestQueriesBot:run --args="BOT_TOKEN https://example.com/verify debug"
```
