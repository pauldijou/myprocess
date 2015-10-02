package models

import java.util.UUID
import java.time.Instant

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.mapping._

trait Model {
  def reference(): Reference
  def id(): UUID = reference.id
}

case class Reference(
  id: UUID = UUID.randomUUID,
  creator: UUID,
  createdAt: Long = 1L, //Instant.now,
  updater: Option[UUID] = None,
  updatedAt: Option[Long] = None
) {
  def toTuple() = (id, creator, createdAt, updater, updatedAt)

  def serialize() = id
}

object Reference {
  def random(): Reference = {
    val id = UUID.randomUUID
    new Reference(id = id, creator = id)
  }

  def by(creator: UUID): Reference = new Reference(creator = creator)

  def by(creator: Reference): Reference = new Reference(creator = creator.id)

  type ReferenceTuple = (UUID, UUID, Long, Option[UUID], Option[Long])

  def fromTuple(tuple: ReferenceTuple): Reference =
    Reference.apply(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  implicit val referenceRule = {
    import play.api.data.mapping.json.Rules._
    Rule.gen[JsValue, Reference]
  }

  implicit val referenceWrite = {
    import play.api.data.mapping.json.Writes._
    Write.gen[Reference, JsObject]
  }
}
