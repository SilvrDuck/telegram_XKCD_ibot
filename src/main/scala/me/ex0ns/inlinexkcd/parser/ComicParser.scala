package me.ex0ns.inlinexkcd.parser

import me.ex0ns.inlinexkcd.models.Comic

import scala.concurrent.Future

/** Abstract parser interface for fetching and parsing comics from various
  * sources
  *
  * Implementations should handle:
  *   - HTTP requests to comic source API/website
  *   - Parsing response data into Comic model
  *
  * Good practices that should also be considered:
  *   - Duplicate detection and error handling
  *   - Bulk fetching
  */
trait ComicParser {

  /** Fetch and parse a single comic by its ID
    *
    * @param id
    *   The ID/number of the comic to fetch
    * @return
    *   Future containing Either an Exception (duplicate or fetch error) or the
    *   parsed Comic
    */
  def parseID(id: Int): Future[Either[Exception, Comic]]

  /** Fetch and parse all comics from the source in bulk
    *
    * @param step
    *   The number of comics to fetch
    * @return
    *   Stream of failed fetch counts per batch
    */
  def parseAll(step: Int = 10): Stream[Int]
}
