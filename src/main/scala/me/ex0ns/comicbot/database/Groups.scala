package me.ex0ns.comicbot.database

import com.typesafe.scalalogging.Logger
import me.ex0ns.comicbot.models.Group
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final object Groups extends Collection[Group] with Database {
  override def ct = implicitly

  final class InvalidGroupJSON extends Exception

  private val codecRegistry = fromRegistries(fromProviders(classOf[Group]), DEFAULT_CODEC_REGISTRY )
  private val codecDB = database.withCodecRegistry(codecRegistry)

  override val collection: MongoCollection[Group] = codecDB.getCollection("groups")
  override val logger = Logger(LoggerFactory.getLogger(Groups.getClass))

  /**
    * Insert a new group to the database
    *
    * @param group the Group object to insert
    */
  override def insert(group: Group): Future[Group] = {
    collection.insertOne(group).head().map(_ => group)
  }

  /**
    * Insert a new group to the database by ID
    *
    * @param groupId the ID of the group
    */
  def insertById(groupId: String): Future[Group] = {
    val group = Group(groupId.toLong)
    insert(group)
  }

  /**
    * Remove a group from the database
    * @param groupId the id of the group to remove
    */
  def remove(groupId: String) =
    collection.deleteOne(equal("id", groupId.toLong)).toFuture()

  /**
    * Find all the documents in the collection
    * @return  all the document in the collection
    */
  def all =
    collection
      .find()
      .toFuture()
      .map((documents) => documents)

}
