package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.mapping._

object Permission extends Enumeration {
  type Permission = Value

  implicit val permissionRule: Rule[JsValue, Permission] = Rule.fromMapping {
    case JsString(name) => try {
      Success(withName(name))
    } catch {
      case e: NoSuchElementException => Failure(Seq(ValidationError("error.invalid.permission.name", name)))
    }
    case js => Failure(Seq(ValidationError("error.invalid.permission", Json.stringify(js))))
  }

  implicit val permissionWrite: Write[Permission, JsValue] = Write[Permission, JsValue] { permission =>
    JsString(permission.toString)
  }

  val ADMIN, CREATE_USER, EDIT_USER, DELETE_USER = Value

}
