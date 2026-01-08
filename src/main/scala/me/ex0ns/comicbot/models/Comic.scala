package me.ex0ns.comicbot.models


import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.methods.{ParseMode, SendMessage, SendPhoto}
import com.bot4s.telegram.models.InputFile
import me.ex0ns.comicbot.helpers.StringHelpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final case class Comic(_id: Int,
                       title: String,
                       img: String,
                       num: Int,
                       alt: Option[String],
                       link: Option[String],
                       transcript: Option[String],
                       views: Option[Int])
  extends Notification {
  def getBoldTitle: String = title.bold

  override def notify(group: Group, noteTemplate: Option[String])(implicit request: RequestHandler[Future]): Future[Unit] = {
    val formattedNote = noteTemplate match {
      case Some(template) =>
        template
          .replace("{title}", title)
          .replace("{alt}", alt.getOrElse(""))
          .replace("{link}", link.getOrElse(""))
          .replace("{num}", num.toString)
          .replace("{img}", img)
          .replace("{transcript}", transcript.getOrElse(""))
      case None =>
        alt.getOrElse("") + link.map(x => s"\n\n$x").getOrElse("")
    }

    for {
      response <- Future { scalaj.http.Http(img).asBytes }
      if response.isSuccess
      bytes = response.body
      photo = InputFile(img.split('/').last, bytes)
      _ <- request(SendMessage(group.id, getBoldTitle, Some(ParseMode.Markdown)))
      _ <- request(SendPhoto(group.id, photo))
      _ <- request(
        SendMessage(group.id,
          formattedNote,
          Some(ParseMode.Markdown),
          disableWebPagePreview = Some(true)))
    } yield ()
  }
}
