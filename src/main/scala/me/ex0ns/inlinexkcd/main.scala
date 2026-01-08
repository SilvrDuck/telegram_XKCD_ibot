package me.ex0ns.inlinexkcd

import me.ex0ns.inlinexkcd.bot.InlineComicBot
import me.ex0ns.inlinexkcd.config.BotConfig
import me.ex0ns.inlinexkcd.parser.ComicParser
import me.ex0ns.inlinexkcd.comics.xkcd.{XKCDConfig, XKCDParser}

import scala.io.Source
import scala.util.Properties

object main {

  final case class ComicSource(config: BotConfig, parserFactory: () => ComicParser)

  val sources: Map[String, ComicSource] = Map(
    "xkcd" -> ComicSource(XKCDConfig.config, () => new XKCDParser())
  )

  private def token: String =
    Properties
      .envOrNone("TELEGRAM_KEY")
      .filter(_.nonEmpty)
      .getOrElse(Source.fromFile("telegram.key").getLines().next())

  def main(args: Array[String]): Unit = {
    val cli = Cli.parse(args).fold(msg => { println(msg); sys.exit(1) }, identity)
    val src = sources.getOrElse(cli.comic, {
      println(s"Unknown comic '${cli.comic}'. Available: ${sources.keys.mkString(", ")}")
      sys.exit(1)
    })

    cli.command match {
      case Cli.Command.RunBot =>
        new InlineComicBot(token, src.config, src.parserFactory()).run()

      case Cli.Command.Parse(idOpt) =>
        val p = src.parserFactory()
        idOpt match {
          case Some(id) => p.parseID(id)
          case None => p.parseAll(src.config.bulkFetchStep)
        }
    }
  }
}
