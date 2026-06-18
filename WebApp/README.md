# WebApp

A multiplatform bot (JVM server + JS WebApp) that demonstrates Telegram WebApp integration.

## Functionality

The JVM part hosts a Ktor HTTP server that serves a static WebApp frontend and exposes REST
endpoints for inline query submission, WebApp data validation, custom emoji status setting, and
prepared keyboard button management. The JS part is the WebApp itself — a single-page app with a
button that communicates back to the bot and adapts to the user's Telegram theme and viewport.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2 | `WEB_APP_URL` | Public HTTPS URL where the WebApp is hosted |
| 3 *(optional)* | `PORT` | Port for the Ktor server (default: `8080`) |

Optional flags:

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |

## Bot Commands

| Command | Description |
|---------|-------------|
| `/reply_markup` | Send a reply keyboard containing a WebApp button |
| `/inline` | Send an inline keyboard containing a WebApp button |
| `/attachment_menu` | Send an attachment-menu WebApp button |
| `/prepareKeyboard` | Retrieve and display the saved prepared inline keyboard button |

## Server Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/inline` | Accept an inline query result submitted from the WebApp |
| `POST` | `/check` | Validate the `initData` signature sent by the WebApp |
| `POST` | `/setCustomEmoji` | Set a custom emoji status for the user based on WebApp data |
| `POST` | `/getPreparedKeyboardButtonId` | Return the ID of a previously saved prepared inline keyboard button |

## Capabilities

- Serves the compiled JS WebApp as static files
- HMAC-SHA256 validation of Telegram WebApp `initData`
- Custom emoji status setting via `setUserEmojiStatus`
- Prepared inline keyboard button saved with `savePreparedInlineMessage`
- Supports all three WebApp button surfaces: reply keyboard, inline keyboard, attachment menu
- Requires a domain with valid SSL (or a Telegram test account)
- JVM server + Kotlin/JS frontend in a single Gradle multiplatform project

## Launch

```bash
./gradlew :WebApp:run --args="BOT_TOKEN https://your.domain.com 8080"
```
