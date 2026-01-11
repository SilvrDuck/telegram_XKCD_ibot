package me.ex0ns.comicbot.models

import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.methods.{ParseMode, SendMessage, SendPhoto}
import com.bot4s.telegram.models.InputFile
import com.typesafe.scalalogging.Logger
import me.ex0ns.comicbot.helpers.StringHelpers._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final case class Comic(
    _id: Int,
    title: String,
    img: String,
    num: Int,
    alt: Option[MdV2],
    link: Option[String],
    transcript: Option[String],
    views: Option[Int]
) extends Notification {

  private val logger = Logger(LoggerFactory.getLogger(classOf[Comic]))

  override def notify(group: Group, noteTemplate: Option[String])(implicit
      request: RequestHandler[Future]
  ): Future[Unit] = {
    // alt is MdV2: already MarkdownV2-ready (escaped at creation time)
    // Other fields are plain text and need escaping
    logger.info(s"[DEBUG] Comic #$num - raw title: $title")
    logger.info(s"[DEBUG] Comic #$num - title.bold: ${title.bold}")
    logger.info(s"[DEBUG] Comic #$num - alt.map(_.value): ${alt.map(_.value)}")
    logger.info(s"[DEBUG] Comic #$num - link: $link")

    val formattedNote = noteTemplate match {
      case Some(template) =>
        template
          .replace("{title}", title.escaped)
          .replace("{alt}", alt.map(_.value).getOrElse(""))
          .replace("{link}", link.getOrElse("").escaped)
          .replace("{num}", num.toString)
          .replace("{img}", img.escaped)
          .replace("{transcript}", transcript.getOrElse("").escaped)
      case None =>
        alt.map(_.value).getOrElse("") + link.map(l => s"\n\n${l.escaped}").getOrElse("")
    }

    logger.info(s"[DEBUG] Comic #$num - formattedNote: $formattedNote")

    for {
      response <- Future { scalaj.http.Http(img).asBytes }
      if response.isSuccess
      bytes = response.body
      photo = InputFile(img.split('/').last, bytes)
      _ <- request(SendMessage(group.id, title.bold, Some(ParseMode.MarkdownV2)))
      _ <- request(SendPhoto(group.id, photo))
      _ <- request(
        SendMessage(
          group.id,
          formattedNote,
          Some(ParseMode.MarkdownV2),
          disableWebPagePreview = Some(true)
        )
      )
    } yield ()
  }
}
