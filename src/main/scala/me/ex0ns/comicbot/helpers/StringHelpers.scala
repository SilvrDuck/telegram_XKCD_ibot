package me.ex0ns.comicbot.helpers

object StringHelpers {

  private def escapeMdV2(s: String): String = {
    val specialChars = "_*[]()~`>#+-=|{}.!"
    s.replace("\\", "\\\\")
      .flatMap { c =>
        if (specialChars.contains(c)) s"\\$c"
        else c.toString
      }
  }

  implicit class MarkdownString(string: String) {
    def escaped: String = escapeMdV2(string)
    def bold: String = s"*${escapeMdV2(string)}*"
    def italic: String = s"_${escapeMdV2(string)}_"
    def urlWithAlt(alt: String): String = s"[${escapeMdV2(alt)}](${safeMdV2Url(string)})"
    def altWithUrl(url: String): String = s"[${escapeMdV2(string)}](${safeMdV2Url(url)})"
    def inlineCode: String = s"`$string`"
    def blockCode: String = s"```$string```"
  }

  def safeMdV2Url(url: String): String =
    url.replace("\\", "\\\\").replace(")", "\\)")

}
