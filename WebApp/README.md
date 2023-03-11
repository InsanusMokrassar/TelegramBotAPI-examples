# WebApp

Here you may find simple example of `WebApp`. For work of this example you will need one of two things:

* Your own domain with SSL (letsencrypt is okay)
* Test account in telegram

What is there in this module:

* JVM part of this example is a server with simple static webapp sharing and bot which just gives the webapp button to open webapp
* JS part is the WebApp with one button and reacting to chaged user theme and app viewport

## How to run

```bash
./gradlew run --args="TOKEN WEB_APP_ADDRESS"
```
