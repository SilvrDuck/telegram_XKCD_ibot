package me.ex0ns.comicbot.models

import com.bot4s.telegram.api.RequestHandler
import me.ex0ns.comicbot.database.Groups

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Notification {

  val MESSAGES_LIMIT        = 30
  val MESSAGE_ORDER_DELAY   = 200
  val MESSAGES_LIMIT_TIME   = 1000

  def notify(group: Group, noteTemplate: Option[String])(implicit request: RequestHandler[Future]): Future[Unit]
  def notifyAllGroups(noteTemplate: Option[String])(implicit request: RequestHandler[Future]) : Future[Unit] =
    Groups.all.map((groups) => {
      groups
        .grouped(MESSAGES_LIMIT)
        .foreach((groupCluster) => {
          Future.traverse(groupCluster)(g => notify(g, noteTemplate))
          Thread.sleep(MESSAGES_LIMIT_TIME)
        })
    })

}
