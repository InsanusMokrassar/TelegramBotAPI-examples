# ReactionsInfoBot

A long-polling example that handles Telegram's per-user reaction updates and anonymous reaction-count updates. It has no commands and does not respond to ordinary messages.

## Behavior

When an identifiable user changes their reactions on a message, the bot:

1. temporarily adds a `✍` reaction to the original message;
2. sends that user a private message which externally replies to the reacted message;
3. lists the user's new reaction set, including ordinary emoji, rendered custom emoji with its custom-emoji ID, a generic label for paid reactions, and fallback information for unknown reaction types;
4. removes its temporary reaction after the private reply succeeds.

If the user removed all reactions, the private message contains only its heading. Updates where a chat is the reaction actor are not handled by this per-user trigger. Telegram also does not deliver reaction-change updates caused by bots, so the temporary `✍` reaction does not trigger this handler recursively.

For anonymous reaction-count updates, the bot fetches and prints the extended chat plus the raw count update to standard output. It does not send a Telegram message for those updates, which Telegram may deliver after a delay.

If adding `✍` is not allowed, the private-message step is not reached. If the private message fails, the final cleanup is not reached and the bot's temporary reaction can remain on the original message.

## Telegram setup, permissions, and privacy

- Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
- Add the bot to each group, supergroup, or channel to watch and promote it to administrator. Telegram requires administrator status for both `message_reaction` and `message_reaction_count` updates; the long-polling setup explicitly requests these update types.
- Enable reactions in the watched chat and allow the `✍` emoji so the bot can apply its temporary marker.
- Each user who should receive reports must first open the bot's private chat and press **Start**. The bot has no `/start` response, but Telegram otherwise prevents it from initiating a private conversation. Reports also fail if the user blocks the bot.
- BotFather privacy mode may remain enabled. It controls group-message delivery, not reaction updates.

The example uses long polling and automatically removes an existing webhook at startup. Run only one update consumer for the token at a time.

## Arguments

The first argument is the required bot token. Debug logging is enabled only when the second argument is exactly `debug`; later occurrences are ignored.

## Launch

From the repository root:

```bash
./gradlew :ReactionsInfoBot:run --args="<BOT_TOKEN>"
```

```bash
./gradlew :ReactionsInfoBot:run --args="<BOT_TOKEN> debug"
```
