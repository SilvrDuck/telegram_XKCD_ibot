package me.ex0ns.comicbot.comics.darthsanddroids

import com.typesafe.scalalogging.Logger
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Node, TextNode}
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import me.ex0ns.comicbot.database.Comics
import me.ex0ns.comicbot.database.Comics.DuplicatedComic
import me.ex0ns.comicbot.models.{Comic, MdV2}
import me.ex0ns.comicbot.parser.ComicParser
import me.ex0ns.comicbot.helpers.HtmlToMarkdownV2
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DarthsAndDroidsParser extends ComicParser {

  private val backend = AsyncHttpClientFutureBackend()
  private val logger = Logger(
    LoggerFactory.getLogger(classOf[DarthsAndDroidsParser])
  )

  private val BASE_URL = "https://www.darthsanddroids.net"

  private def episodeUrl(id: Int): String = {
    val paddedId = f"$id%04d"
    s"$BASE_URL/episodes/$paddedId.html"
  }

  private def extractIntroMarkdown(
      doc: org.jsoup.nodes.Document
  ): Option[String] = {
    val textDiv = doc.select("div.text").first()
    if (textDiv == null) {
      logger.debug("No div.text found")
      return None
    }

    val transcriptH3 =
      textDiv.select("h3").asScala
        .find(_.text().trim.equalsIgnoreCase("Transcript"))
        .orNull

    if (transcriptH3 == null) {
      logger.debug("No Transcript h3 found in div.text")
      return None
    }

    // Collect HTML elements before Transcript
    val allChildren = textDiv.children().asScala.toList
    val introElements = allChildren
      .takeWhile(_ != transcriptH3)
      .filter(el => el.tagName() == "p" || el.tagName() == "blockquote")

    if (introElements.isEmpty) {
      logger.debug("No intro elements found")
      return None
    }

    // Convert each element to Markdown and join
    val markdown = introElements
      .map(HtmlToMarkdownV2.convert)
      .mkString("")
      .trim

    logger.debug(s"Converted HTML to Markdown, length: ${markdown.length}")
    Option(markdown).filter(_.nonEmpty)
  }

  private def fetchHtml(id: Int): Future[Either[Exception, String]] = {
    basicRequest
      .get(uri"${episodeUrl(id)}")
      .header("User-Agent", "ComicBot/0.1 (Telegram Bot)")
      .send(backend)
      .map { response =>
        if (response.code.code == 404) {
          Left(new Exception("404"))
        } else if (!response.code.isSuccess) {
          Left(new Exception(s"HTTP ${response.code.code}"))
        } else {
          response.body.left.map(new Exception(_))
        }
      }
  }

  private def parseHtml(html: String, id: Int): Either[Exception, Comic] = {
    try {
      // Parse with BASE_URL so absUrl() works in markdown conversion
      val doc = Jsoup.parse(html, BASE_URL)

      val centerDiv = doc.select("div.center").first()
      if (centerDiv == null) return Left(new Exception("div.center not found"))

      val titleElement = centerDiv.select("b").first()
      if (titleElement == null)
        return Left(new Exception("title <b> not found"))
      val rawTitle = titleElement.text()
      val title = rawTitle.replaceFirst("^Episode \\d+:\\s*", "")

      val imgElement = centerDiv.select("img").first()
      if (imgElement == null) return Left(new Exception("img not found"))

      val imgUrl = {
        val srcset = imgElement.attr("srcset")
        if (srcset.nonEmpty) {
          srcset.split(",").head.split("\\s+").head
        } else {
          imgElement.attr("src")
        }
      }

      val absoluteImgUrl =
        if (imgUrl.startsWith("http")) imgUrl
        else s"$BASE_URL$imgUrl"

      // Extract intro blurb (from div.text before Transcript) as Markdown
      val introAlt = extractIntroMarkdown(doc).map(MdV2.alreadyFormatted)
      logger.debug(
        s"Episode $id - Intro alt length: ${introAlt.map(_.value.length).getOrElse(0)}"
      )
      if (introAlt.isDefined) {
        logger.debug(
          s"Episode $id - Intro alt preview: ${introAlt.get.value.take(100)}"
        )
      }
      val imgAlt = Option(imgElement.attr("alt")).filter(_.nonEmpty).map(MdV2.fromPlain)
      // Use intro blurb as alt, fallback to image alt
      val alt = introAlt.orElse(imgAlt)
      logger.debug(
        s"Episode $id - Final alt length: ${alt.map(_.value.length).getOrElse(0)}"
      )

      val transcript = {
        val h3Elements = doc.select("h3")
        val transcriptH3 =
          h3Elements.asScala.find(_.text().toLowerCase.contains("transcript"))
        transcriptH3.flatMap { h3 =>
          Option(h3.nextElementSibling())
            .filter(_.tagName() == "p")
            .map(_.wholeText().trim) // Use wholeText() to keep line breaks
            .filter(_.nonEmpty)
        }
      }

      val link = Some(episodeUrl(id))
      Right(
        Comic(
          _id = id,
          title = title,
          img = absoluteImgUrl,
          num = id,
          alt = alt,
          link = link,
          transcript = transcript,
          views = Some(0)
        )
      )

    } catch {
      case e: Exception =>
        logger.error(s"Error parsing HTML for episode $id: ${e.getMessage}")
        Left(e)
    }
  }

  override def parseID(id: Int): Future[Either[Exception, Comic]] = {
    Comics.exists(id).flatMap {
      case true =>
        logger.debug(s"Episode $id already exists")
        Future.successful(Left(new DuplicatedComic))
      case false =>
        fetchHtml(id).flatMap {
          case Right(html) =>
            parseHtml(html, id) match {
              case Right(comic) =>
                Comics.insert(comic).map(_ => Right(comic))
              case Left(e) =>
                Future.successful(Left(e))
            }
          case Left(e) =>
            logger.debug(s"Failed to fetch episode $id: ${e.getMessage}")
            Future.successful(Left(e))
        }
    }
  }

  private def bulkFetch(startingPage: Int, size: Int): Int = {
    Range(startingPage, startingPage + size)
      .map { id =>
        val result = Await.result(parseID(id), Duration.Inf)
        Thread.sleep(1000) // Being real nice here
        result
      }
      .count(_.isLeft)
  }

  override def parseAll(step: Int = 20): Stream[Int] = {
    Stream
      .from(1, step)
      .map(x => bulkFetch(x, step))
      .takeWhile(_ != step)
      .force
  }
}
