# GiftsBot

Lists gifts owned by the current chat and looks up individual collectible gifts by name.

## Behavior

At startup the bot prints its `getMe` result, then receives updates through long polling. While handling `/start`, it
shows a typing action, retrieves every page of gifts, and chooses the request from the command chat type:

- a business chat uses the business connection ID and requests the connected business account's gifts;
- a private chat requests that user's gifts;
- a public or unknown chat type requests that chat's gifts.

Regular gifts are shown with their ID, optional text, and Stars cost. Unique gifts are shown with their optional ID,
name, model, and number. Long results are split into multiple Telegram messages; an empty result produces
`This chat have no any gifts`.

The bot also handles `UniqueGiftInfo` service messages with `onUniqueGiftSentOrReceived`. It logs and replies with
Bot API 10.3's `text`, parsed `textSources` (`entities`), and `isPrivate` fields. When entities are present, the reply
reuses them so the gift text keeps its formatting.

## Commands

- `/start` — lists the owned gifts selected by the current chat type. It must be the only command in the message and
  takes no arguments.
- `/unique_gift <name>` calls `getUniqueGift` and replies with the collectible's number, model, symbol, backdrop,
  and Telegram link. Use the name after `t.me/nft/` in a collectible's link, or copy the name from a `/start` result.
  The lookup works independently of gift ownership.

Other commands and ordinary non-command messages are ignored; unique-gift service messages are handled separately.

## Setup and permissions

1. Create a bot with BotFather and obtain its token.
2. For private-chat use, have the user start the bot so it can receive `/start` and reply.
3. For group or channel use, add the bot and allow it to receive the command and send messages in that chat.
4. For business-chat use, enable the bot's Business/Secretary mode, connect it to the business account, and grant the
   **View gifts and Stars** (`can_view_gifts_and_stars`) business right.

The lookup reports Telegram's error description when available. The owned-gift listing requires the applicable chat
or business access.

## Arguments and launch

The first application argument is the required bot token. Optional, exact, case-sensitive flags may follow in either
order: `debug` prints KSLog diagnostics to standard output, and `testServer` selects Telegram's Bot API test server.
Unknown trailing arguments are ignored.

From the repository root:

```bash
./gradlew :GiftsBot:run --args="<BOT_TOKEN>"
```
