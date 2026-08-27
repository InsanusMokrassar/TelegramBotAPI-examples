# LinkPreviewsBot

A long-polling example that resends text-bearing content with every demonstrated
`LinkPreviewOptions` variant.

## Behavior

The bot handles every content message. It searches the message's text entities for
the first plain URL or text-link entity. When one is found, it sends the same text
and entities to the same chat seven times:

- with link previews disabled;
- with a large preview above the text;
- with a large preview below the text;
- with a small preview above the text;
- with a small preview below the text;
- with Telegram's default preview size above the text;
- with Telegram's default preview size below the text.

The detected URL is selected explicitly for each enabled preview. If the content is
not text-bearing or contains no URL entity, the bot replies that only content with
a URL is supported. It defines and registers no commands.

## Setup and permissions

1. Create a bot, obtain its token, and keep the token private.
2. Start a private chat with it, or add it to a chat where previews should be tested.
3. Ensure Telegram delivers the relevant content messages and the bot may send
   messages in that chat.

The example uses no administrator-only methods and does not configure a webhook.

## Run

From the repository root:

```bash
./gradlew :LinkPreviewsBot:run --args="BOT_TOKEN"
```

The first argument is the required bot token. The optional, case-sensitive second
argument `debug` enables formatted library logging on standard output. Arguments
after the second are ignored.

```bash
./gradlew :LinkPreviewsBot:run --args="BOT_TOKEN debug"
```
