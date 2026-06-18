# WebHooks

A bot that uses a Telegram webhook instead of long polling, served via an embedded Ktor HTTP server.

## Functionality

Registers a webhook URL with Telegram, starts a Ktor server on the configured port, and processes
incoming updates through that server. Responds to `/start` with information about the active webhook
configuration.

## Arguments

| Position | Value | Description |
|----------|-------|-------------|
| 1 | `BOT_TOKEN` | Telegram bot token |
| 2+ | `https://...` | One or more HTTPS URLs to register as the webhook URL |

Additional optional arguments (any order, after token and URL):

| Value | Description |
|-------|-------------|
| `debug` | Enable verbose debug logging |
| `testServer` | Connect to the Telegram test server instead of production |
| *any number* | Port to listen on (e.g. `8080`); defaults to `8080` |
| *any other string* | Sub-path to mount the webhook route on (e.g. `it/is/subpath`) |

### Example

```
BOT_TOKEN https://sample.com it/is/subpath 8080
```

- Webhook registered as `https://sample.com/it/is/subpath`
- Ktor listens on `0.0.0.0:8080` at path `/it/is/subpath`

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Replies with current webhook URL and configuration details |

## Capabilities

- Full webhook integration via `setWebhook` + Ktor route
- Configurable listening port and sub-path
- Optional debug mode
- Runs via Ktor embedded server (not long polling)

## Launch

```bash
../gradlew run --args="BOT_TOKEN https://sample.com it/is/subpath 8080"
```
