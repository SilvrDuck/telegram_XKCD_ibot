package me.ex0ns.inlinexkcd

object Cli {

  sealed trait Command
  object Command {
    case object RunBot extends Command
    final case class Parse(id: Option[Int]) extends Command
  }

  final case class Parsed(comic: String = "xkcd", command: Command = Command.RunBot)

  private val usage: String =
    """Usage:
      |  inlinexkcd [--comic <name>] [parse [id]]
      |
      |Examples:
      |  inlinexkcd
      |  inlinexkcd --comic xkcd
      |  inlinexkcd parse
      |  inlinexkcd --comic xkcd parse 123
      |""".stripMargin

  def parse(args: Array[String]): Either[String, Parsed] = {
    def parseId(s: String): Either[String, Int] =
      s.toIntOption.filter(_ >= 0).toRight(s"Invalid id '$s' (expected a non-negative integer)\n\n$usage")

    args.toList match {
      case Nil =>
        Right(Parsed())

      case List("-h") | List("--help") =>
        Left(usage)

      case List("-c" | "--comic", comic) =>
        Right(Parsed(comic = comic))

      case List("parse") =>
        Right(Parsed(command = Command.Parse(None)))

      case List("parse", id) =>
        parseId(id).map(n => Parsed(command = Command.Parse(Some(n))))

      case List("-c" | "--comic", comic, "parse") =>
        Right(Parsed(comic = comic, command = Command.Parse(None)))

      case List("-c" | "--comic", comic, "parse", id) =>
        parseId(id).map(n => Parsed(comic = comic, command = Command.Parse(Some(n))))

      case _ =>
        Left(s"Invalid arguments.\n\n$usage")
    }
  }
}
