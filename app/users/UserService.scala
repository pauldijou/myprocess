package services

import java.util.UUID
import play.api.libs.json._
import play.api.data.mapping._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import slick.backend.DatabaseConfig
import db.Driver

import models.{Reference, User}

class Users (protected val dbConfig: DatabaseConfig[Profile])(implicit ec: ExecutionContext) {
  import Driver.api._

  val db = dbConfig.db
  val users = Users.users

  def all(): Future[Seq[User]] = db.run(Users.allUsersCompiled("").result)

  def findById(id: UUID): Future[Option[User]] = db.run(Users.userByIdCompiled(id).result).map(_.headOption)

  def findByEmail(email: String): Future[Option[User]] = db.run(Users.userByEmailCompiled(email).result).map(_.headOption)

  // Insert user
  def insert(user: User): Future[User] =
    db.run(users += user).map(_ => user)
}

object Users extends ReferenceSchema {
  import Driver.api._

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS") with ReferenceFields {
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def role = column[UUID]("ROLE")

    def roleFK = foreignKey("ROLE_FK", role, Roles.roles)(_.id)
    def uniqueEmail = index("UNIQUE_EMAIL", email, unique = true)

    def * = (
      ref_*, name, email, password, role
    ).shaped <> ({ case (ref, name, email, password, role) =>
      User(Reference.fromTuple(ref), name, email, password, role)
    }, { user: User =>
      Some((user.reference.toTuple, user.name, user.email, user.password, user.role))
    })
  }

  val users = TableQuery[UsersTable]
  val schema = users.schema

  // Find users
  def allUsers(fake: Rep[String]) = for {
    user <- users
  } yield user

  val allUsersCompiled = Compiled(allUsers _)

  // Find user by id
  def userById(id: Rep[UUID]) = for {
    user <- users if user.id === id
  } yield user

  val userByIdCompiled = Compiled(userById _)

  // Find user by email
  def userByEmail(email: Rep[String]) = for {
    user <- users if user.email === email
  } yield user

  val userByEmailCompiled = Compiled(userByEmail _)
}
