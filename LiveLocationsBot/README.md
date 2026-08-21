# LiveLocationsBot

A long-polling example that sends, updates, and stops a live-location message.

## Commands and behavior

- `/start` begins a live-location sequence in the command's chat.
- `Cancel`, an inline button on the live-location message, stops that sequence.

The generated coordinates are synthetic: the first update uses latitude and
longitude `(0.0, 0.0)`, and both values increase by `1.0` for each later update.
The bot emits an update immediately and then every three seconds.

While the sequence runs, the bot tracks its current location message. It accepts
only callback data equal to `cancel` from that same message. After a matching button
press, it cancels the update job, stops the live location, and removes the button.

Every received update is printed to standard output. The `/start` handler is not
separately registered in Telegram's command menu.

## Setup and permissions

1. Create a bot, obtain its token, and keep the token private.
2. Start a private chat with the bot, or add it to a group where the demo should run.
3. Allow the bot to send location messages in that chat.

The coordinates do not come from the user's device. The example needs no
administrator-only methods and does not configure a webhook.

## Run

From the repository root:

```bash
./gradlew :LiveLocationsBot:run --args="BOT_TOKEN"
```

The first argument is the required bot token. Omitting it causes startup to fail;
additional arguments are ignored. Stop the process with `Ctrl+C`.
