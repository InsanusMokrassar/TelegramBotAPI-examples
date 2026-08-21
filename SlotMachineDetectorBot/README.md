# SlotMachineDetectorBot

A long-polling example that distinguishes slot-machine dice from other Telegram
dice animations and decodes the slot reels.

## Trigger and output

The bot handles every dice message and defines no commands.

- For a slot-machine dice, it calls `calculateSlotMachineResult` and replies in the
  exact format `<left-reel>|<center-reel>|<right-reel>`.
- If a slot-machine value cannot be decoded, the handler sends no reply.
- For every other dice animation, it replies
  `There is no slot machine dice in message`.

The example reports the three decoded reels only. It does not calculate a numeric
score or decide whether the combination wins.

## Setup, permissions, and privacy

1. Create a bot, obtain its token, and keep the token private.
2. Start a private chat with it, or add it to a group where dice should be observed.
3. Allow the bot to send replies in that chat.

Dice are ordinary non-command messages. In groups, configure Telegram's bot privacy
setting so the desired dice updates are delivered; disable privacy mode if the bot
should inspect all group dice. No administrator-only API methods are used. The bot
stores no message or dice data.

## Run

From the repository root:

```bash
./gradlew :SlotMachineDetectorBot:run --args="BOT_TOKEN"
```

The first argument is the required bot token. Omitting it causes startup to fail;
additional arguments are ignored. This launcher has no debug or test-server flag.
