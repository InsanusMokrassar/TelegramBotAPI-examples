# WidgetsBot

WidgetsBot sends an interactive die inside a Telegram message. Tap **Roll a die** to get a private answer with your result. The bot also saves the number in the widget so anyone viewing that message can see the last roll.

## Run

Create a bot with BotFather and open its private chat in a Telegram client that supports widgets. From the repository root, run the long-polling bot with its token. [BotFather](https://t.me/BotFather)

```bash
./gradlew :WidgetsBot:run --args="<BOT_TOKEN>"
```

The optional `debug` argument enables diagnostic logging. Telegram currently supports widgets in private chats with the bot in production. To try groups, forwards, or inline results, use a bot token and account created in Telegram's test environment and add `testServer` after the token. [Test account setup](https://core.telegram.org/bots/features#creating-a-bot-in-the-test-environment) · [Widget availability](https://core.telegram.org/bots/widgets#availability)

## Try the widgets

| Command | What to try |
| --- | --- |
| `/start` | Show the two widget commands. |
| `/widget` | Send a die and tap it. Send a second one to see that each message keeps its own last roll in `chatData`. |
| `/sharedwidget` | Send two messages showing the same die. Tap either one and watch both last-roll values change to match. On the test server, forward one and roll it from the forward. |

`sendWidget` creates the shared die with `isShared = true` and `allowQueries = true`. The bot reads `widget.id` from the returned message and passes it to `sendSharedWidget` for the second message.

To try inline mode, send `/setinline` to BotFather on the test server, select the bot, and enter a placeholder. In a private chat or group, type `@your_bot_username` followed by a space and select **Roll a die**. Each selected `InputWidgetMessageContent` starts a new die. [Telegram inline setup](https://core.telegram.org/bots/inline) · [Inline widgets](https://core.telegram.org/bots/widgets#inline-mode-widgets)

A tap in a bot-sent or inline message reaches `onWidgetQuery`. A tap in a forward of the shared die reaches `onSharedWidgetQuery`. The bot calls `answerWidgetQuery` for the tapping viewer, then writes the roll with `editMessageWidgetData` using either the chat and message IDs or the inline message ID. For a shared die it calls `editSharedWidgetData` with the widget ID. Open copies call `watchPublicData()` to refresh on Telegram's polling interval. The example requests a five-second interval when saving a shared roll. [Telegram widget guide](https://core.telegram.org/bots/widgets#requests-to-the-bot)
