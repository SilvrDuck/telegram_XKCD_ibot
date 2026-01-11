package me.ex0ns.comicbot.models

import me.ex0ns.comicbot.helpers.StringHelpers._
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

/**
 * MarkdownV2-ready text for Telegram.
 * The value is already escaped and can be sent directly.
 */
final case class MdV2 private (value: String) extends AnyVal

object MdV2 {

  /** Create from plain text - escapes special chars */
  def fromPlain(s: String): MdV2 = MdV2(s.escaped)

  /** Create from already formatted MarkdownV2 text (e.g., from HtmlToMarkdownV2) */
  def alreadyFormatted(s: String): MdV2 = MdV2(s)

  /** MongoDB codec: stores the value as-is (already escaped) */
  class MdV2Codec extends Codec[MdV2] {
    override def encode(writer: BsonWriter, value: MdV2, encoderContext: EncoderContext): Unit = {
      writer.writeString(value.value)
    }

    override def decode(reader: BsonReader, decoderContext: DecoderContext): MdV2 = {
      MdV2(reader.readString())
    }

    override def getEncoderClass: Class[MdV2] = classOf[MdV2]
  }
}
