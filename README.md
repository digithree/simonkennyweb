# simonkennyweb

Personal website, written in Kotlin with Ktor. Displays in quirky Terminal CSS with faux command line navigation.

## Configuration

Easily deployable to any service which can run a `jar`.

Must however set the following properties:

1. `ktor.security.airtableApiKey` - Airtable API key
2. `ktor.security.friendCode` - Friend code for friend access

use for example

```
java -jar dist/simonkennyweb-shadow.jar -P:ktor.security.airtableApiKey="XXXXXXXXXXXXX" -P:ktor.security.friendCode="YYYYYYYYYYY"
```

### Config vars in Heroku

The `Procfile` is set up for Heroku and expects the following config vars to be set:

1. `AIRTABLE_API_KEY`
2. `FRIEND_CODE`

These are set respectively to the above properties when starting the `jar`
