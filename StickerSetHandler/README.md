# StickerSetHandler

StickerSetHandler is a Kotlin/JVM long-polling example that creates and manages one Telegram sticker set for each
private chat. It copies stickers sent to the bot into a set owned by the user.

## Behavior

| Trigger | Action |
| --- | --- |
| `/start` | Replies with a short hint for the `/delete` command. |
| `/delete` | Deletes the chat's entire sticker set and replies `Deleted`; if deletion fails, it replies that it could not delete the set. |
| A regular, mask, or custom-emoji sticker | Downloads and re-uploads the sticker. The first supported sticker creates the set; later compatible stickers are added to it. The bot replies with the created or newly added sticker. |
| Any other update | Performs no chat action. Every update is still printed to standard output. |

The deterministic set name is `s<chat_id>_by_<bot_username>`, and a newly created set is titled
`Sticker set by <bot_first_name>`. The original sticker format, emoji (or a smiling fallback), mask position, and the
initial custom-emoji repainting setting are preserved where applicable. Sticker keywords are not copied. Sticker
types cannot be mixed in one set: the first sticker determines whether the set is regular, mask, or custom emoji.

After `/delete`, sending another supported sticker recreates the set. Sticker objects that the library does not
recognize are ignored.

## Telegram setup and permissions

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Open a private chat with the bot and start it.
3. Send a sticker, or use `/start` to display the available command.

No Telegram administrator rights or special BotFather modes are required for this private-chat workflow. The
process needs network access and a writable temporary directory because it downloads each sticker before uploading
it to Telegram.

Use the example in private chats only. The code passes the incoming chat ID as the sticker-set owner's user ID;
private-chat IDs identify the user, while group and channel IDs do not. Telegram also enforces its own account,
sticker-type, format, and sticker-set limits, so an otherwise supported request can still be rejected.

## Arguments

| Position | Argument | Required | Description |
| --- | --- | --- | --- |
| 1 | `BOT_TOKEN` | Yes | Bot API token issued by BotFather. |

The program reads only the first argument. It does not implement `debug`, `testServer`, or other optional flags, and
it exits during startup if the token is omitted.

## Run

From the repository root:

```bash
./gradlew :StickerSetHandler:run --args="<BOT_TOKEN>"
```

Keep the real token out of source control and be aware that command-line arguments may be visible in shell history
or process listings. The bot polls until the process is stopped.

## Safety notes

- `/delete` has no confirmation step and removes the complete set, not just its most recent sticker.
- The set name contains the numeric private-chat/user ID. That identifier can be exposed when the sticker-set name
  or link is displayed or shared.
- There is no allowlist, rate limit, or moderation. Every supported sticker is downloaded to temporary storage and
  uploaded again, so expose the bot only with suitable API, bandwidth, and disk limits.
- Every received update is written to standard output, and several failure paths print stack traces. Those logs can
  contain user, chat, message, and file metadata and should be protected accordingly.
- Any sticker-set lookup failure is treated as if the set were absent, so a temporary Telegram or network failure
  can lead to a failed creation attempt rather than a retry.
