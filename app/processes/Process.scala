package models

import play.api.libs.json._
import play.api.data.mapping._
import play.api.libs.functional.syntax._

case class Process(
  reference: Reference,
  title: String,
  description: String,
  root: Step
) extends Model

object Process {
  implicit val processRule = {
    import play.api.data.mapping.json.Rules._
    Rule.gen[JsValue, Process]
  }

  implicit val processWrite: Write[Process, JsObject] = {
    import play.api.data.mapping.json.Writes._
    Write.gen[Process, JsObject]
  }
}
