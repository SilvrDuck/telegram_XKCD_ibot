package me.ex0ns.comicbot.comics.darthsanddroids

import me.ex0ns.comicbot.config.BotConfig

object DarthsAndDroidsConfig {
  val config: BotConfig = BotConfig(
    sourceName = "darthsanddroids",
    cronSchedule = "0 */15 9-23 * * ?",
    noteTemplate = Some("{alt}\n\n\n{link}"),
    searchFields = Seq("title", "transcript", "alt"),
    searchWeights = Some(Map("title" -> 10, "transcript" -> 5, "alt" -> 3)),
    bulkFetchStep = 20
  )
}
