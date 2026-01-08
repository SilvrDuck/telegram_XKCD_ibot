package me.ex0ns.comicbot.comics.xkcd

import me.ex0ns.comicbot.config.BotConfig

object XKCDConfig {
  val config: BotConfig = BotConfig(
    sourceName = "xkcd",
    cronSchedule = "0 */15 9-23 * * ?",
    noteTemplate = Some("{alt}\n\nhttps://explainxkcd.com/{num}"),
    searchFields = Seq("transcript", "title", "alt"),
    searchWeights = Some(Map("title" -> 10, "transcript" -> 5, "alt" -> 1)),
    bulkFetchStep = 10
  )
}
