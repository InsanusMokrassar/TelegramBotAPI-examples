# StarTransactionsBot

A bot that demonstrates Telegram Stars payments: sending invoices, handling transactions, and
delivering paid media.

## Functionality

Sends a 1-star invoice on `/start`. After successful payment the bot sends paid media (a photo
and a video). The admin can browse the full transaction history with pagination. Refunds received
from Telegram are logged. Checkout queries are validated before approval.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2 | `ADMIN_USER_ID` | Numeric Telegram user ID that is allowed to view transaction history |

Optional flags (any order after the required arguments):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Send a 1-star invoice to the user |
| `/transactions` | Browse paginated star transaction history *(admin only)* |

## Capabilities

- Creates and sends a Stars invoice via `sendInvoice`
- Handles `PreCheckoutQuery` events to approve or reject checkout
- Delivers paid media (photo + video) after a successful payment
- Paginates transaction history using inline keyboard next/previous buttons
- Tracks and logs refund notifications
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN ADMIN_USER_ID"
```
