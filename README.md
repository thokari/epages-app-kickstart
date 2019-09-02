# epages-app-kickstart

This is a small example app, that implements the installation process as documented on
https://developer.epages.com/apps/install-app.html .

Besides that, it doesn't really do much, but it could be used as a starting point.  
There exists a minimal implementation of an ePages-API-client.

## Setup

In order to run the tests, you need to start a Postgres database, for example
using docker.  
See https://hub.docker.com/_/postgres .

Once the DB is running, you can run `./gradlew test` to run the tests.  
Use `./gradlew run` to start the application.

## Testing the appstore

The simplest way to install an app into your shop would be to use the "Private Apps" feature:
 - create a private app in the backend of your shop
 - enter an "Application Callback URL" that represents your app, and is reachable from the location of your shop
 - set the credentials (`clientId` and `clientSecret`) in the file
`config.test.json`.
 - you might also need to change the `appHostname`, depending on your particular setup, and `callbackPath`, depending on what you entered before
 - run the app, use the "Test authorisation" button, verify the log output of the app
