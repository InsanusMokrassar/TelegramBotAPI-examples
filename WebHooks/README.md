# WebHooks

Launches webhook-based simple bot. Use `/start` with bot to get simple info about webhooks

## Launch

```bash
../gradlew run --args="BOT_TOKEN https://sample.com it/is/subpath 8080 debug"
```

Required arguments:

1. Token
2. Arguments starting with `https://`

Optional arguments:

* Any argument == `debug` to enable debug mode
* Any argument **not** starting with `https://` and **not** equal to `debug` as **subpath** (will be used as
subroute to place listening of webhooks)
* Any argument as number of port

Sample: `TOKEN https://sample.com it/is/subpath 8080` will result to:

* `TOKEN` used as token
* Bot will set up its webhook info as `https://sample.com/it/is/subpath`
* Bot will set up to listen webhooks on route `it/is/subpath`
* Bot will start to listen any incoming request on port `8080` and url `0.0.0.0`
