# WebHooks

A webhook-based example bot with an embedded Ktor CIO server. It registers a public webhook URL with Telegram, accepts update POSTs on a matching local route, and dispatches them through a behavior context; it does not use long polling.

## Server and bot behavior

- The server binds plain HTTP to `0.0.0.0` on the selected port (`8080` by default) and blocks until it is stopped.
- With no subpath, webhook POSTs are handled at `/`. With a subpath such as `telegram/hooks`, they are handled at `/telegram/hooks`, and that path is appended to the registered public address.
- An accepted update POST receives HTTP `200`; malformed JSON or a synchronous dispatch failure receives HTTP `500`. Later handler exceptions are printed by the behavior context. No health-check or other application route is installed.
- The bot registers all supported update types. Pending updates are not explicitly dropped when the webhook is set, and the webhook remains registered after this process exits.
- `/start` works only in a private chat and replies with the configured external address, local bind host (`0.0.0.0`), and port. The displayed address does not include the optional subpath. The command is ignored in groups.

The example does not configure a webhook `secret_token` and does not validate Telegram's secret-token header. Anyone who can reach the endpoint can submit update-shaped JSON, so protect the endpoint at the network/proxy layer or add secret-token verification before production use.

## TLS, certificates, and networking

The embedded server has no HTTPS connector and loads no certificate. The `setWebhook` call also uploads no public certificate. In a typical deployment:

1. Point a public DNS name at a reverse proxy or load balancer reachable from Telegram.
2. Terminate HTTPS there with a publicly trusted certificate.
3. Forward the webhook path to this process over HTTP on its internal port.
4. Allow Telegram to reach the public listener through any firewall/NAT rules. Telegram webhook listeners use public ports `443`, `80`, `88`, or `8443`; the internal forwarded port can remain `8080`.

Self-signed-certificate deployment is not supported by this example as written because it neither serves TLS nor passes a certificate to `setWebhook`. The external address must start with `https://`; avoid a trailing slash when also supplying a subpath, otherwise the registered URL contains a doubled slash.

## Arguments and environment

Create a bot with [@BotFather](https://t.me/BotFather) and obtain its token. All runtime configuration then comes from positional command-line arguments; no environment variables are read.

- The first argument is the required bot token.
- The first argument beginning with `https://` is the required public address.
- `debug` anywhere enables formatted default KSLog output.
- The first integer-valued argument becomes the local port; otherwise it is `8080`.
- The first argument after the token that is neither the chosen address nor exactly `debug` becomes the optional subpath. A leading `/` is normalized for the public URL.

The parser also treats a numeric port argument as a subpath candidate. Therefore `TOKEN ADDRESS 9000` registers `/9000` and listens on port `9000`; the current CLI cannot select a custom port while keeping the webhook route at `/`. Put a nonnumeric subpath before the port when both are wanted. Additional arguments are otherwise ignored.

## Launch

From the repository root, the minimal form uses `/` and port `8080`:

```bash
./gradlew :WebHooks:run --args="<BOT_TOKEN> https://bot.example.com"
```

Example with a subpath, custom internal port, and debug logging:

```bash
./gradlew :WebHooks:run --args="<BOT_TOKEN> https://bot.example.com telegram/hooks 9000 debug"
```

That command registers `https://bot.example.com/telegram/hooks` and binds the forwarded POST route as `/telegram/hooks` on local port `9000` across all network interfaces.
