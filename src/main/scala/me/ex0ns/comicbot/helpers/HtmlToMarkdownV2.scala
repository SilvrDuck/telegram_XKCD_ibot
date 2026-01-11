package me.ex0ns.comicbot.helpers

import me.ex0ns.comicbot.helpers.StringHelpers._
import org.jsoup.nodes.{Element, Node, TextNode}
import scala.jdk.CollectionConverters._

/**
 * Converts HTML to Telegram MarkdownV2 format.
 *
 * Telegram's HTML parse mode has a very limited subset (no <br>, <p>, <div>,
 * <ul>, etc.), so we convert HTML to MarkdownV2 instead. This gives us a
 * single consistent format across all comic sources and full control over
 * formatting.
 *
 * Output is ready to send directly to Telegram - all escaping is handled via
 * StringHelpers.
 *
 * MarkdownV2 spec: https://core.telegram.org/bots/api#markdownv2-style
 */
object HtmlToMarkdownV2 {

  def convert(element: Element): String = renderNode(element)

  private def renderChildren(e: Element): String =
    e.childNodes().asScala.map(renderNode).mkString

  private def renderNode(n: Node): String = n match {
    case t: TextNode =>
      t.text().escaped

    case e: Element =>
      e.tagName().toLowerCase match {
        case "br" => "\n"

        case "b" | "strong" =>
          s"*${renderChildren(e)}*"

        case "i" | "em" =>
          s"_${renderChildren(e)}_"

        case "a" =>
          val text = renderChildren(e)
          val href = Option(e.absUrl("href"))
            .filter(_.nonEmpty)
            .getOrElse(e.attr("href"))
          if (href.nonEmpty) s"[$text](${safeMdV2Url(href)})" else text

        case "blockquote" =>
          // Each line in blockquote starts with > (not escaped)
          val inner = renderChildren(e).trim
          inner.split("\n").map(line => s">$line").mkString("\n")

        case "p" =>
          val content = renderChildren(e).trim
          if (content.nonEmpty) s"$content\n\n" else ""

        case "div" =>
          renderChildren(e).trim

        case "ul" | "ol" =>
          e.select("> li").asScala
            .map(li => s"• ${renderChildren(li).trim}")
            .mkString("\n") + "\n\n"

        case "li" =>
          renderChildren(e).trim

        case _ =>
          renderChildren(e)
      }

    case _ => ""
  }
}
