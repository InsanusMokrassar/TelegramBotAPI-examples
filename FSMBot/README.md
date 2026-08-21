# FSMBot

FSMBot demonstrates the finite-state-machine (FSM) support provided by
[MicroUtils](https://github.com/InsanusMokrassar/MicroUtils) and TelegramBotAPI's
behaviour builder.

## Behaviour

1. Send `/start` to begin a conversation chain for the current chat.
2. The bot asks for content and waits in the same forum topic/thread in which the
   chain was started.
3. Each content message is copied back to the chat, then the bot waits again.
4. Send `/stop` in that topic/thread to end the chain and receive a confirmation.

FSM state is held in memory, so active chains are lost when the process stops.
Incoming updates and state-handling errors are printed to standard output.

## Commands

- `/start` — start or restart the content-resending chain.
- `/stop` — stop the active chain while the bot is waiting for content.

The bot does not register its command menu automatically; commands can be typed
directly or configured separately with BotFather.

## Requirements and permissions

- A bot token obtained from BotFather.
- A compatible JDK for the repository's Gradle wrapper.
- Permission to send messages and the content types being copied in the target chat.
- No administrator rights are required. For use in groups, disable privacy mode if
  the bot must receive arbitrary non-command messages rather than only commands and
  other updates Telegram exposes to privacy-enabled bots.

## Run

From the repository root:

```bash
./gradlew :FSMBot:run --args="<BOT_TOKEN>"
```

`BOT_TOKEN` is the required first positional argument. The bot uses long polling;
no webhook or additional configuration is needed.
