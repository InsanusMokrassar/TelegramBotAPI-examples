# BusinessConnectionsBot

A comprehensive bot that demonstrates the Telegram Business Account API, including message
management, profile editing, star transfers, story posting, and gift listing.

## Functionality

The bot connects to a business account. When a business connection is established it maps the
business chat ID to the owner's personal chat so that management commands can be used in the
personal chat. Messages received via the business connection are forwarded to the owner.
Typing `PIN` or `UNPIN` in a business message pins or unpins it. A wide set of management commands
is available in the owner's PM.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |

Optional arguments (any order after the token):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/get_business_account_info` | Print account name, username, bio, and other details |
| `/set_business_account_name` | Set the account's first and last name (prompts for input) |
| `/set_business_account_username` | Set the account's username (prompts for input) |
| `/set_business_account_bio` | Set the account bio (auto-resets to the old value after 15 seconds) |
| `/set_business_account_profile_photo` | Set a private profile photo (send a photo in reply) |
| `/set_business_account_profile_photo_public` | Set a public profile photo (send a photo in reply) |
| `/get_business_account_star_balance` | Show the current star balance of the business account |
| `/transfer_business_account_stars` | Transfer stars from the business account to the bot |
| `/get_business_account_gifts` | List all gifts received by the business account |
| `/post_story` | Post a story with a link area (send a photo in reply) |
| `/delete_story` | Delete the most recently posted story |

## Capabilities

- `BusinessConnection` event handling: maps business chat IDs to personal owner chats
- Forwards business messages to the owner's PM
- PIN / UNPIN keyword detection to pin or unpin messages in the business chat
- Business message deletion tracking
- Mutex-protected concurrent access to the chat mapping
- Story creation with `InputStoryContentPhoto` and `StoryAreaTypeLink`
- Checklist content support
- Runs via long polling

## Launch

```bash
../gradlew run --args="BOT_TOKEN"
```
