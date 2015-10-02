package services

import java.util.UUID
import play.api.libs.json._
import play.api.data.mapping._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import slick.backend.DatabaseConfig
import db.Driver

import models.{Reference, Role, Permission}

class Roles (protected val dbConfig: DatabaseConfig[Profile])(implicit ec: ExecutionContext) {
  import Driver.api._

  val db = dbConfig.db
  val roles = Roles.roles

  def all(): Future[Seq[Role]] = db.run(Roles.allRolesCompiled("").result)

  def findById(id: UUID): Future[Option[Role]] = db.run(Roles.roleByIdCompiled(id).result).map(_.headOption)

  def findByName(name: String): Future[Option[Role]] = db.run(Roles.roleByNameCompiled(name).result).map(_.headOption)

  // Insert role
  def insert(role: Role): Future[Role] = {
    db.run(roles += role).map(_ => role)
  }
}

object Roles extends ReferenceSchema {
  import Driver.api._

  class RolesTable(tag: Tag) extends Table[Role](tag, "ROLES") with ReferenceFields {
    def name = column[String]("NAME")
    def permissions = column[JsValue]("PERMISSIONS")

    def uniqueName = index("UNIQUE_NAME", name, unique = true)

    def * = (
      ref_*, name, permissions
    ).shaped <> ({ case (ref, name, jsPermissions) =>
      val permissions = jsPermissions match {
        case JsArray(values) => values.map(Permission.permissionRule.validate).map(_.asOpt).collect {
          case Some(perm) => perm
        }
        case _ => throw new RuntimeException("")
      }
      Role(Reference.fromTuple(ref), name, permissions)
    }, { role: Role =>
      Some((role.reference.toTuple, role.name, new JsArray(role.permissions.map(Permission.permissionWrite.writes))))
    })
  }

  val roles = TableQuery[RolesTable]
  val schema = roles.schema

  // Find roles
  def allRoles(fake: Rep[String]) = for {
    role <- roles
  } yield role

  val allRolesCompiled = Compiled(allRoles _)

  // Find role by id
  def roleById(id: Rep[UUID]) = for {
    role <- roles if role.id === id
  } yield role

  val roleByIdCompiled = Compiled(roleById _)

  // Find role by name
  def roleByName(name: Rep[String]) = for {
    role <- roles if role.name === name
  } yield role

  val roleByNameCompiled = Compiled(roleByName _)
}
