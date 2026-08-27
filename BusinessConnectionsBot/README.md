# Business Connections Bot

This example demonstrates how a bot can manage a connected Telegram Business account. It handles business-connection updates, mirrors business messages, exposes inline actions for marking messages as read or deleting them, and exercises account, Stars, gifts, stories, and checklist APIs.

This is a feature demonstration, not a production-ready bot. Several commands change the connected account or transfer its Stars, and the bot keeps connection IDs only in memory.

## Telegram setup and rights

1. Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token.
2. Enable Business/Secretary Mode for the bot in BotFather. Telegram's name for the setting can vary by client version.
3. Start this example, then connect the bot to a Telegram Business account and allow it to manage the desired private chats. The account owner should also open a private chat with the bot; all commands below are accepted only there.
4. Grant the business rights needed by the features you want to try:

| Business right | Used by |
| --- | --- |
| Reply/send messages (`can_reply`) | Mirroring and replying to business messages and resending checklists |
| Read messages (`can_read_messages`) | **Read message** inline button |
| Delete all messages (`can_delete_all_messages`) | **Delete message** for incoming customer messages |
| Delete sent messages (`can_delete_sent_messages`) | Deleting messages sent by the bot itself |
| Edit name (`can_edit_name`) | `/set_business_account_name` |
| Edit username (`can_edit_username`) | `/set_business_account_username` |
| Edit bio (`can_edit_bio`) | `/set_business_account_bio` |
| Edit profile photo (`can_edit_profile_photo`) | Both profile-photo commands |
| View gifts and Stars (`can_view_gifts_and_stars`) | Balance and gift-list commands |
| Transfer Stars (`can_transfer_stars`) | `/transfer_business_account_stars` |
| Manage stories (`can_manage_stories`) | `/post_story` and `/delete_story` |

The account-management methods used here do not require the connected account to have Telegram Premium as of Bot API 9.0. Sending checklists still depends on the account and client being able to create them. Enable Bot-to-Bot Communication Mode in BotFather if you want to exercise the special reply path for a bot that contacts the managed business account.

See Telegram's [business-bot overview](https://core.telegram.org/bots/features#business-mode) and [`BusinessBotRights`](https://core.telegram.org/bots/api#businessbotrights) for the platform rules. The Bot API generally restricts business replies and reads to private chats active in the last 24 hours.

## Launch

From the repository root, pass the token as the first application argument:

```bash
./gradlew :BusinessConnectionsBot:run --args="<BOT_TOKEN>"
```

Pass the literal `debug` as the optional second argument to print verbose library logs:

```bash
./gradlew :BusinessConnectionsBot:run --args="<BOT_TOKEN> debug"
```

The bot prints its own `getMe` result, discards updates accumulated before startup, and then starts long polling. Because business connection IDs are cached only in memory, run the bot before creating/enabling the connection. After a restart, disable and re-enable the connection if owner commands do not respond.

## Automatic business update handling

- When a business connection is enabled or disabled, the bot records/removes its IDs and notifies the account owner in their private chat.
- A text business message starting with `/pin` or `/unpin` pins or unpins the accessible message it replies to.
- Other new business messages are resent to the sender's chat and receive a short diagnostic reply. Incoming customer messages also produce a notification in the business owner's bot chat with **Read message** and **Delete message** buttons.
- When the sender is another bot, the example sends a bot-to-bot diagnostic reply and skips the owner notification.
- Edited business messages are resent with an edit diagnostic. Deleted-business-message updates are reported to the account owner with the affected chat and message IDs.
- A received checklist is resent to the same chat on behalf of the business account when the connection ID can be resolved.

The inline **Read message** button calls `readBusinessMessage`. **Delete message** calls `deleteBusinessMessages`; for an incoming customer message, this requires the right to delete all managed-chat messages.

## Private-chat commands

These commands must be sent by the connected account owner in their private chat with the bot. Except for `/get_business_account_info`, handlers silently stop when that chat is not associated with an in-memory business connection.

| Command | Behavior |
| --- | --- |
| `/get_business_account_info` | Prints the current `BusinessConnection` as formatted JSON, or reports that no connection is known. |
| `/set_business_account_name <first_name> [last_name]` | Changes the connected account's first name and optional last name. Each name is parsed as one whitespace-separated argument. |
| `/set_business_account_username <username>` | Changes the account username to the single supplied argument. |
| `/get_business_account_star_balance` | Prints the account's current Telegram Stars balance. |
| `/transfer_business_account_stars <count>` | Transfers an integer number of Stars from the business account to the bot. Telegram accepts values from 1 through 10,000; this example leaves range validation to Telegram. |
| `/get_business_account_gifts` | Fetches every page of owned gifts and prints their Kotlin representations, splitting long output across messages. |
| `/set_business_account_bio <text>` | Saves the current bio, sets the complete text after the command as the new bio, waits 15 seconds, and attempts to restore the saved value. An empty text clears it temporarily. |
| `/set_business_account_profile_photo` | Reply to a photo with this command to set it as the main profile photo; after 15 seconds the bot removes that photo. |
| `/set_business_account_profile_photo_public` | Reply to a photo to set it as the public profile photo; after 15 seconds the bot removes it. |
| `/post_story` | Reply to a photo, video, or live photo to post it as a six-hour story with a fixed test caption and link area. |
| `/delete_story` | Reply to a story message to delete that story. Telegram only permits the bot to delete stories it posted for the business account. |

The profile-photo cleanup removes the newly current photo; it does not upload a saved copy of the previous photo. Telegram may promote the previous main photo after removal. Also note that the success text from `/post_story` currently mentions `/remove_story`; the implemented deletion command is `/delete_story`.
