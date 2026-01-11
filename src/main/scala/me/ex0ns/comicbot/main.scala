package me.ex0ns.comicbot

import me.ex0ns.comicbot.bot.InlineComicBot
import me.ex0ns.comicbot.config.BotConfig
import me.ex0ns.comicbot.parser.ComicParser
import me.ex0ns.comicbot.comics.xkcd.{XKCDConfig, XKCDParser}
import me.ex0ns.comicbot.comics.darthsanddroids.{
  DarthsAndDroidsConfig,
  DarthsAndDroidsParser
}

import scala.io.Source
import scala.util.Properties
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object main {

  final case class ComicSource(
      config: BotConfig,
      parserFactory: () => ComicParser
  )

  val sources: Map[String, ComicSource] = Map(
    "xkcd" -> ComicSource(
      XKCDConfig.config,
      () => new XKCDParser()
    ),
    "darthsanddroids" -> ComicSource(
      DarthsAndDroidsConfig.config,
      () => new DarthsAndDroidsParser()
    )
  )

  private def token: String =
    Properties
      .envOrNone("TELEGRAM_KEY")
      .filter(_.nonEmpty)
      .getOrElse(Source.fromFile("telegram.key").getLines().next())

  def main(args: Array[String]): Unit = {
    val cli =
      Cli.parse(args).fold(msg => { println(msg); sys.exit(1) }, identity)
    val src = sources.getOrElse(
      cli.comic, {
        println(
          s"Unknown comic '${cli.comic}'. Available: ${sources.keys.mkString(", ")}"
        )
        sys.exit(1)
      }
    )

    cli.command match {
      case Cli.Command.RunBot =>
        new InlineComicBot(token, src.config, src.parserFactory()).run()

      case Cli.Command.Parse(idOpt) =>
        val p = src.parserFactory()
        idOpt match {
          case Some(id) => p.parseID(id)
          case None     => p.parseAll(src.config.bulkFetchStep)
        }
    }
  }
}
