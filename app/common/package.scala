import java.util.UUID
import java.time.Instant
import play.api.data.mapping._
import play.api.libs.json._
import play.api.libs.functional.syntax._

package object models {
  def jsonAs[T](f: PartialFunction[JsValue, Validation[ValidationError, T]])(args: Any*) =
     Rule.fromMapping[JsValue, T](
       f.orElse{ case j => play.api.data.mapping.Failure(Seq(ValidationError("validation.invalid", args: _*)))
     })

  implicit val uuidRule: Rule[JsValue, UUID] = jsonAs[UUID] {
    case JsString(v) => play.api.data.mapping.Success(UUID.fromString(v))
  }("UUID")

  implicit val uuidWrite: Write[UUID, JsValue] = Write(uuid => JsString(uuid.toString))

  implicit val instantRule: Rule[JsValue, Instant] = jsonAs[Instant] {
    case JsString(v) => play.api.data.mapping.Success(Instant.parse(v))
  }("Instant")

  implicit val instantWrite: Write[Instant, JsValue] = Write(instant => JsString(instant.toString))
}

package object controllers {

}

package object services {
  type Profile = slick.driver.JdbcProfile
}
