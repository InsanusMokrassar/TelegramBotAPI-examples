# ForwardInfoSenderBot

This example uses long polling to inspect the forward metadata of every content message
delivered to the bot and replies with a short description of its source.

## Behavior

There are no bot commands. Send or forward any content message that Telegram delivers to
the bot. The reply depends on the message's `forwardInfo`:

- messages without forward metadata produce `There is no forward info`;
- anonymous forwards show the sender signature;
- user and bot forwards show the sender type, numeric ID, name, and username when present;
- channel forwards show the channel title, linked when the channel has a public username;
- supergroup forwards show the group title;
- messages sent on behalf of a channel show that channel's title.

The response uses Telegram text entities to format identifiers and source names. The bot
can only report metadata that Telegram includes in the received message.

## Setup and permissions

Obtain a bot token and make sure the bot can receive the messages you want to inspect and
send replies in that chat. It does not request administrator privileges or use persistent
storage. Telegram's bot privacy and chat permissions still determine which group messages
are delivered and whether the reply can be sent.

Run from the repository root:

```bash
./gradlew :ForwardInfoSenderBot:run --args="<BOT_TOKEN>"
```

The token is the required first application argument. Additional arguments are ignored;
omitting the token makes startup fail. The process keeps polling until it is stopped.
