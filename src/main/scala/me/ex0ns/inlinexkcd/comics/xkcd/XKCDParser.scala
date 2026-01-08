package me.ex0ns.inlinexkcd.comics.xkcd

import com.typesafe.scalalogging.Logger
import sttp.client3._
import me.ex0ns.inlinexkcd.database.Comics
import me.ex0ns.inlinexkcd.database.Comics.DuplicatedComic
import me.ex0ns.inlinexkcd.models.Comic
import me.ex0ns.inlinexkcd.parser.ComicParser
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

class XKCDParser extends ComicParser {

  private val backend = AsyncHttpClientFutureBackend()
  private val logger = Logger(LoggerFactory.getLogger(classOf[XKCDParser]))

  /**
    * Fetch XKCD comic based on its ID
    *
    * @param id the ID of the strip to fetch
    * @return a future that may contain the HttpResponse (if successful)
    */
  def parseID(id: Int): Future[Either[Exception, Comic]] = {
    val document = Comics.exists(id)
    document.flatMap {
      case true =>
        logger.debug(s"Document with id: $id already exists")
        Future.successful(Left(new DuplicatedComic))
      case false =>
        basicRequest.get(uri"https://xkcd.com/$id/info.0.json")
          .send(backend)
          .map(_.body)
          .flatMap {
            case Right(comic) => Comics.insert(comic).map(comic => Right(comic))
            case Left(e) =>
              logger.error(s"Unable to retrieve comic: $e")
              Future.successful(Left(new Exception(s"Unable to retrieve comic: $e")))
          }
    }
  }

  /**
    * Convert a Future[T] to Future[Try] to be able to count the number of failed/successful Future
    */
  private def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
    f.map(Success(_))
      .recover({
        case e => logger.warn(e.toString); Failure(e)
      })

  /**
    * Fetch in parallel size pages, starting from startingPage, and returns the number of failed Future
    */
  private def bulkFetch(startingPage: Int, size: Int) = {
    val t = Range(startingPage, startingPage + size)
      .take(size)
      .map(parseID)
      .map(futureToFutureTry)
    val r = Await.result(Future.sequence(t), Duration.Inf)
    r.count(_.isFailure)
  }

  /**
    * Fetch and parse all XKCD comics
    */
  def parseAll(step: Int = 10): Stream[Int] = {
    Stream
      .from(0, step)
      .map(x => bulkFetch(x, step))
      .takeWhile(_ != step)
      .force
  }

}
