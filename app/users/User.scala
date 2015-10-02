package models

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.mapping._

case class User (
  reference: Reference,
  name: String,
  email: String,
  password: String,
  role: UUID
) extends Model

object User {
  implicit val userRule = {
    import play.api.data.mapping.json.Rules._
    Rule.gen[JsValue, User]
  }

  implicit val userWrite: Write[User, JsObject] = {
    import play.api.data.mapping.json.Writes._
    Write.gen[User, JsObject]
  }
}
