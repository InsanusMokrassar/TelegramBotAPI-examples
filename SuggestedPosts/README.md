# SuggestedPosts

A long-polling playground for channel direct messages and suggested-post lifecycle
updates.

## Commands and content triggers

- `/start` fetches and prints full information for the command's chat. It sends no
  reply and is not registered in Telegram's command menu.
- Every delivered channel-direct-message content message is printed with its chat
  information, then resent to the same direct-message chat with empty
  `SuggestedPostParameters` so it becomes a suggested post.
- Channel paid-post content is printed to standard output.

## Suggested-post lifecycle

For each detected suggested post, the bot races three branches:

1. wait for a suggested-post-approved event from the same chat;
2. wait for a suggested-post-declined event from the same chat;
3. after one-second delays, send `3`, `2`, and `1`, then decline the post.

A matching approval or decline ends the countdown branch. The waits correlate by
chat ID rather than individual suggested-post message ID, which matters when several
suggestions are active in the same chat. The bot does not approve posts itself and
does not set a price or scheduled publication time.

Lifecycle events are printed and receive these replies:

- paid → `Paid`;
- approved → `Approved`;
- declined → `Declined`;
- refunded → `Refunded`;
- approval failed → `Approval failed`.

Every update is also printed. State exists only in active coroutine waits; restarting
the bot cancels pending countdowns and forgets active suggestions.

## Setup and permissions

Use a channel with direct messages and suggested posts enabled. Add the bot with
enough channel access to receive direct-message updates, resend their content, send
countdown/reply messages, and decline suggested posts. The code does not validate
these permissions at startup and configures no webhook.

Full updates, chat details, suggested content, and payment-related events are logged.
Use a private test channel and protect process output.

## Run

From the repository root:

```bash
./gradlew :SuggestedPosts:run --args="BOT_TOKEN"
```

The token is the required first argument. Optional exact flags may follow it in any
order: `debug` enables formatted logging, and `testServer` selects Telegram's test
environment. Other arguments are ignored.
