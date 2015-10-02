package services

import java.util.UUID
import java.time.Instant
import db.Driver

trait ReferenceSchema {
  import Driver.api._

  trait ReferenceFields { self: Table[_] =>
    def id = column[UUID]("ID", O.PrimaryKey)
    def creator = column[UUID]("CREATOR")
    def createdAt = column[Long]("CREATED_AT")
    def updater = column[Option[UUID]]("UPDATER")
    def updatedAt = column[Option[Long]]("UPDATED_AT")

    def ref_* = (id, creator, createdAt, updater, updatedAt)

    // def matchRef(tup: ReferenceFields.RefColumnTuple) = {
    //   (id is tup._1)
    // }
  }
}
