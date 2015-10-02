package models

import Permission._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.mapping._

case class Role (
  reference: Reference,
  name: String,
  permissions: Seq[Permission]
) extends Model {
  def can(permissions: Seq[Permission]): Boolean = (this.permissions intersect permissions).size > 0
}

object Role {
  implicit val roleRule = {
    import play.api.data.mapping.json.Rules._
    Rule.gen[JsValue, Role]
  }

  implicit val roleWrite: Write[Role, JsObject] = {
    import play.api.data.mapping.json.Writes._
    Write.gen[Role, JsObject]
  }
}
