# WebApp

A Kotlin Multiplatform Telegram Web App showcase. One JVM process serves the
compiled browser client, exposes helper routes, and runs the bot with long polling.

## Bot and server behavior

The server binds `0.0.0.0` on the configured port. It serves the production JS
distribution from `WebApp/build/dist/js/productionExecutable`, falling back to
`developmentExecutable`; startup fails if neither directory exists.

Bot handlers include:

- `/reply_markup` — a one-time reply-keyboard Web App button;
- `/inline` — an inline Web App button with a small link preview below the text;
- `/attachment_menu` — an inline Web App button with a large preview above the text;
- `/prepareKeyboard` — saves a managed-bot request button for the current user in
  an in-memory map; it sends no confirmation;
- any other command — help for `/inline` and `/reply_markup`;
- inline queries — an **Open webApp** results button;
- write-access-allowed events with a Web App name — a thank-you message.

Only `/reply_markup` and `/inline` are registered in Telegram's command menu. Bot
information and every update are printed to standard output.

## HTTP routes

| Route | Behavior |
| --- | --- |
| `GET /*` | Serves the compiled Web App and uses `index.html` as the default file. |
| `POST /inline` | Reads plain request text plus `webAppQueryIdField`, then answers that Web App query with a `Result` article containing the text. |
| `POST /check` | Validates serialized Web App init data and returns `true` or `false`. |
| `POST /setCustomEmoji` | Validates init data, reads `userIdField`, and asks the bot to set the fixed sample emoji status; returns a Boolean. |
| `POST /getPreparedKeyboardButtonId` | Validates init data and returns the user's saved button ID (`200`), no content (`204`), or forbidden (`403`). |

The three validation routes accept JSON shaped as
`{"data":"<WebApp initData>","hash":"<initData hash>"}`. Prepared button IDs are
process-local and disappear on restart.

This is demo routing, not a hardened public API: `/inline` does not validate init
data, and the two user-specific routes accept a separate caller-supplied user ID
after validating the payload. Add identity binding, authorization, rate limits, and
deployment hardening before exposing these endpoints beyond a controlled example.

## Browser client

The Compose HTML client validates `initData`, displays chat/safety information, and
loads a prepared button ID. Its controls demonstrate:

- direct and bot-mediated custom emoji status changes;
- answering in chat through `/inline` and hiding the software keyboard;
- popups, alerts, confirmation, write/contact access, and closing confirmation;
- prepared `requestChat`, header/background/bottom-bar colors, back/main/secondary
  buttons, and haptic feedback;
- accelerometer, gyroscope, and device-orientation readings at 200 ms intervals;
- cloud, device, and secure storage; and
- logging the supported Telegram Web App events.

Client feature availability depends on the Telegram platform/version and user-granted
permissions. Opening the URL outside Telegram does not provide authenticated init
data.

## Telegram and hosting setup

1. Create a bot and keep its token private.
2. Build the browser distribution and publish the JVM server through a public HTTPS
   origin with a valid certificate; the built-in server itself provides no TLS.
3. Pass that public root URL as `WEB_APP_URL` and configure the bot's Web App/domain
   in BotFather where Telegram requires it.
4. Enable inline mode to test inline queries. Configure attachment-menu/write-access
   integration separately; `/attachment_menu` only sends a button.
5. Use the bot's private chat for reply-keyboard and `/prepareKeyboard` flows. Emoji,
   contact, sensor, storage, and managed-bot examples need compatible clients and
   the relevant user permissions/capabilities.

The client calls helper APIs on `window.location.origin`, so deploy it at the same
origin and root routing as the JVM server. Protect server logs, which contain updates.

## Build and run from the repository root

Build the production browser files explicitly with:

```bash
./gradlew :WebApp:jsBrowserDistribution
```

`runJvm` also triggers that distribution task through `compileKotlinJvm`. Run from
the repository root because static paths are resolved relative to the working
directory:

```bash
./gradlew :WebApp:runJvm --args="BOT_TOKEN https://webapp.example 8080"
```

The first argument is the required bot token, the second is the required public Web
App URL, and the optional third argument is the port (default `8080` if absent or
nonnumeric). Exact `debug` and `testServer` flags are detected anywhere; keep the
token and URL first, and place a custom numeric port third.

```bash
./gradlew :WebApp:runJvm --args="BOT_TOKEN https://webapp.example 8080 debug testServer"
```

No environment variables are read by this example. Stop both the HTTP server and
bot polling with `Ctrl+C`.
