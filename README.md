# XKCD Bot

XKCDBot is a [Telegram](https://telegram.org) bot, made in scala using the [telegrambot4s](https://github.com/mukel/telegrambot4s) library.
The goal is to send/share XKCD comics in a easier way.

### Installation && build

You obviously need Scala and SBT to make it run.

All you need to make it run is a mongoDB server (> 3.x) and a [Telegram Bot key](https://core.telegram.org/bots), this token must be placed into a `telegram.key` file at the root of the project.

The first step is to fill the database:

```
sbt 'run parse'
```

This will create a database called 'xkcd' and a 'comics' collection within it.


Then you can run the bot using ```sbt run``` with no parameters.

### Usage

```bash
# Run the Telegram bot (default comic source: xkcd)
sbt run

# Parse all comics from XKCD into the database
sbt 'run parse'

# Parse a specific comic by ID
sbt 'run parse 123'

# Specify a different comic source
sbt 'run --comic xkcd'
sbt 'run --comic xkcd parse'
sbt 'run --comic xkcd parse 123'
```

### Features

You can search and post XKCD comics using the inline features of the bot ```@xkcdibot search```. If `search` is empty, 
then it will display the latest XKCD (ordered by date). 
Currently, the number of results is limiter to 50.

To enable the inline search for your bot, do the following:

1. Open chat with @BotFather
2. Send: /mybots
3. Select your bot
4. Click "Bot Settings"
5. Click "Inline Mode"
6. Click "Turn on" (if it says "Turn off", it's already on)

If you add the bot to a group, it will then automatically publish every new XKCD as soon as it's available.

### JAR

[SBT Assembly](https://github.com/sbt/sbt-assembly) is used to generate a single portable jar, juste run ```sbt assembly``` to create it.
When deploying the JAR, do not forget to send the ```telegram.key``` file as well (not bundle inside the JAR for security reasons).
 
### Docker

**Note:** The Docker Hub image `ex0ns/inlinexkcd:latest` is deprecated. It's recommended to build the local image instead.

The easiest way to run your own copy of this bot is to use docker-compose, which will build the bot from source and deploy it alongside a MongoDB container.

1. Set up your environment file:
```bash
cp .env.dist .env
# Edit .env and add your TELEGRAM_KEY
```

2. Build and run using either:
```bash
docker-compose up --build
```

The bot will automatically parse all XKCD comics on first startup if the database is empty.

### Adding a New Comic Source

The bot architecture supports multiple comic sources. To add a new comic source, follow these three steps using XKCD as a reference:

#### Step 1: Create a Config File

Create `src/main/scala/me/ex0ns/inlinexkcd/comics/<comic-name>/<ComicName>Config.scala`:

```scala
package me.ex0ns.inlinexkcd.comics.xkcd

import me.ex0ns.inlinexkcd.config.BotConfig

object XKCDConfig {
  val config: BotConfig = BotConfig(
    // Unique identifier for this comic source
    sourceName = "xkcd",

    // Cron expression: when to check for new comics (every 15 min, 9am-11pm)
    cronSchedule = "0 */15 9-23 * * ?",

    // Template for notification messages sent to groups (optional)
    // Available variables: {title}, {alt}, {link}, {num}, {img}, {transcript} ; see Parser below
    noteTemplate = Some("{alt}\n\nhttps://explainxkcd.com/{num}"),

    // Comic fields to make searchable via inline queries
    searchFields = Seq("transcript", "title", "alt"),

    // Search relevance weights (optional) - higher = more important in search results
    // If not provided, all fields have equal weight (1)
    searchWeights = Some(Map("title" -> 10, "transcript" -> 5, "alt" -> 1)),

    // Number of comics to fetch in parallel during bulk parsing
    bulkFetchStep = 10
  )
}
```

See full implementation: [XKCDConfig.scala](src/main/scala/me/ex0ns/inlinexkcd/comics/xkcd/XKCDConfig.scala)

#### Step 2: Create a Parser

The parser's job is to **fetch comic data from the source website and convert it into the `Comic` format** for storage in MongoDB. Each comic source has its own parsing logic to extract the relevant data.

**Comic Model Fields:**
- `_id`: Unique database ID (typically the comic number)
- `num`: Comic number/identifier from the source
- `title`: Comic title
- `img`: Direct URL to the comic image
- `alt`: Alt text or hover text (tooltip text shown on hover)
- `link`: Optional external link related to the comic
- `transcript`: Full text transcript of the comic (useful for search)
- `views`: Optional view count or popularity metric

Create `src/main/scala/me/ex0ns/inlinexkcd/comics/<comic-name>/<ComicName>Parser.scala`:

```scala
package me.ex0ns.inlinexkcd.comics.xkcd

import me.ex0ns.inlinexkcd.parser.ComicParser
import me.ex0ns.inlinexkcd.models.Comic
// ... other imports

class XKCDParser extends ComicParser {

  // Fetch and parse a single comic by ID
  def parseID(id: Int): Future[Either[Exception, Comic]] = {
    Comics.exists(id).flatMap {
      case true => Future.successful(Left(new DuplicatedComic))
      case false =>
        // Fetch comic data from the source (e.g., API or web scraping)
        basicRequest.get(uri"https://xkcd.com/$id/info.0.json")
          .send(backend)
          .map(_.body)
          .flatMap {
            case Right(comic) =>
              // Convert source data to Comic model and insert into DB
              Comics.insert(comic).map(comic => Right(comic))
            case Left(e) => ...
          }
    }
  }

  // Fetch all comics in parallel batches
  def parseAll(step: Int = 10): Stream[Int] = { ... }

  // Format notification message using the template
  def formatNote(comic: Comic, noteTemplate: Option[String]): String = { ... }
}
```

See full implementation: [XKCDParser.scala](src/main/scala/me/ex0ns/inlinexkcd/comics/xkcd/XKCDParser.scala)

#### Step 3: Register the Source

Add your comic source to the `sources` map in [`main.scala`](src/main/scala/me/ex0ns/inlinexkcd/main.scala):

```scala
import me.ex0ns.inlinexkcd.comics.xkcd.{XKCDConfig, XKCDParser}

val sources: Map[String, ComicSource] = Map(
  "xkcd" -> ComicSource(XKCDConfig.config, () => new XKCDParser())
  // Add your new comic here
)
```

### Testing

@TODO

### Contribute

Feel free to contribute, report any bug or submit ideas to improve the bot !

### License

See [License](https://github.com/ex0ns/telegram_XKCD_ibot/blob/master/LICENSE)
