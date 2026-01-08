package me.ex0ns.comicbot.config

/**
  * Configuration for a comic bot instance
  *
  * @param sourceName Name of the comic source (e.g., "xkcd", "darth-droids")
  * @param cronSchedule Cron expression for new comic check schedule
  * @param noteTemplate Optional template for notification text with variables: {title}, {alt}, {link}, {num}, {img}
  * @param searchFields Fields to index for text search (e.g., Seq("title", "alt"))
  * @param searchWeights Optional weight for each search field (higher = more important). If not provided, all fields have equal weight (1).
  * @param bulkFetchStep Number of comics to fetch in parallel during bulk operations
  */
case class BotConfig(
  sourceName: String,
  cronSchedule: String,
  noteTemplate: Option[String] = None,
  searchFields: Seq[String],
  searchWeights: Option[Map[String, Int]] = None,
  bulkFetchStep: Int = 10
)
