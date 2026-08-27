# StarTransactionsBot

A long-polling Telegram Stars playground for invoices, transaction history, paid
media, pre-checkout approval, and refund updates.

## Commands

- `/start` replies with a sample invoice charging `1` Star, payload
  `sample payload`, and a **Pay** button. Matching pre-checkout queries are approved
  automatically.
- `/transactions` works only in the chat whose numeric ID was supplied as the
  second program argument. It replies with the bot's first transaction page.

The commands are not registered in Telegram's command menu.

## Transaction pages

Pages use an offset and a default limit of `10`. Each returned transaction includes
its ID, date, amount, direction (`incoming`, `outgoing`, or `unknown`), and partner
type. The inline keyboard contains:

- `<` when a previous nonnegative offset exists;
- `>` on every page, using `offset + limit`, even when no later records exist.

Selecting either button fetches that page and edits the existing text message. The
initial `/transactions` command is admin-chat filtered; pagination callbacks are not
independently filtered, so keep the resulting message in the private admin chat.
Pagination state is encoded in callback data and is not persisted by the bot.

## Other triggers

- A photo or video is sent back as paid media costing `1` Star.
- A visual gallery is downloaded to temporary files and re-uploaded as one-Star
  paid media, retaining its caption above the media; only photos and videos are used.
- Paid-media-info messages are printed to standard output.
- Refunded-payment events receive a reply containing the payment information.
- Every received update is printed to standard output.

## Setup, permissions, and payment safety

Create a bot, keep its token private, and choose the numeric ID of the user who may
open transaction history. Use that user's private chat, and allow the bot to send
messages, invoices, photos, and videos. The Telegram environment and client must
support Stars and paid media; purchasers need Stars to complete payments.

This is a charging example: `/start` and delivered photo/video content can create
one-Star purchase flows. Use test accounts or Telegram's test environment when
appropriate. The code approves its sample pre-checkout query but performs no
fulfilment after payment. No chat-administrator methods or webhook are configured.

## Run

From the repository root, pass the token and decimal admin ID first:

```bash
./gradlew :StarTransactionsBot:run --args="BOT_TOKEN ADMIN_USER_ID"
```

Both arguments are required; an absent or nonnumeric admin ID stops startup. The
optional, case-sensitive flags `debug` and `testServer` may follow them in either
order to enable formatted logging or Telegram's test API.

```bash
./gradlew :StarTransactionsBot:run --args="BOT_TOKEN ADMIN_USER_ID debug testServer"
```
