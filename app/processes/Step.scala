package models

import play.api.data.validation.ValidationError
import play.api.data.mapping._
import play.api.libs.json._
import play.api.libs.functional.syntax.unlift

sealed trait Step {
  def title(): String
  def description(): String
  def category(): String
  def children(): Seq[Step]
  def isFinal(): Boolean
}

case class ActionStep (
  title: String,
  description: String,
  child: Option[Step]
) extends Step {
  def category() = "ACTION"
  def children() = child.map(c => Seq(c)).getOrElse(Seq.empty)
  def isFinal() = !child.isDefined
}

case class ConfirmStep (
  title: String,
  description: String,
  ok: Step,
  ko: Step
) extends Step {
  def category() = "CONFIRM"
  def children() = Seq(ok, ko)
  def isFinal() = false
}

object Step {
  val typeFailure = play.api.data.mapping.Failure(Seq(Path -> Seq(ValidationError("validation.unknownType"))))

  implicit val stepRule: Rule[JsValue, Step] = From[JsValue] { __ =>
    import play.api.data.mapping.json.Rules._

    (__ \ "category").read[String].flatMap[Step] {
       case "ACTION" => {
         ((__ \ "title").read[String] and
          (__ \ "description").read[String] and
          (__ \ "child").read[Option[Step]])(ActionStep.apply _)
       }
       case "CONFIRM" => {
         ((__ \ "title").read[String] and
          (__ \ "description").read[String] and
          (__ \ "ok").read[Step] and
          (__ \ "ko").read[Step])(ConfirmStep.apply _)
       }
       case _ => Rule(_ => typeFailure)
    }
  }


  implicit val stepWrite: Write[Step, JsValue] = Write[Step, JsValue] { step =>
    import play.api.data.mapping.json.Writes._
    step match {
      case action: ActionStep => Json.obj(
        "title" -> action.title,
        "description" -> action.description,
        "child" -> action.child.map(c => stepWrite.writes(c))
      )
      case confirm: ConfirmStep => Json.obj(
        "title" -> confirm.title,
        "description" -> confirm.description,
        "ok" -> stepWrite.writes(confirm.ok),
        "ko" -> stepWrite.writes(confirm.ko)
      )
      // case s: ActionStep => To[JsValue] { __ =>
      //   (
      //     (__ \ "title").write[String] ~
      //     (__ \ "description").write[String] ~
      //     (__ \ "child").write[Option[Step]]
      //   )(unlift(ActionStep.unapply _))
      // }
      // case s: ConfirmStep => To[JsValue] { __ =>
      //   (
      //     (__ \ "title").write[String] ~
      //     (__ \ "description").write[String] ~
      //     (__ \ "ok").write[Step] ~
      //     (__ \ "ko").write[Step]
      //   )(unlift(ConfirmStep.unapply _))
      // }
    }
  }
}
