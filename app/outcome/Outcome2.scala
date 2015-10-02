package outcome

import scala.concurrent.{ Future, ExecutionContext }
import play.api.mvc.{Result, Results}
import play.api.http.Status
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.i18n.{MessagesApi, Lang}

import play.api.data.mapping._

case class Outcome[T] (
  value: Option[T] = None,
  global: Seq[ValidationError] = Seq(),
  form: Seq[ValidationError] = Seq(),
  fields: Map[String, Seq[ValidationError]] = Map(),
  successStatus: Option[Int] = None,
  failureStatus: Option[Int] = None
) {
  private def hasErrors(): Boolean = !global.isEmpty || !form.isEmpty || !fields.isEmpty

  def isSuccess(): Boolean = value.isDefined && !hasErrors
  def isFailure(): Boolean = !isSuccess

  def value(v: T): Outcome[T] = copy(value = Some(v))

  private def addField(fields: Map[String, Seq[ValidationError]], field: String, error: ValidationError) =
    fields + (field -> fields.get(field).map(_ :+ error).getOrElse(Seq(error)))

  private def addField(fields: Map[String, Seq[ValidationError]], field: String, errors: Seq[ValidationError]) =
    fields + (field -> fields.get(field).map(_ ++ errors).getOrElse(errors))

  def appendGlobal(error: String, args: Any*): Outcome[T] = appendGlobal(ValidationError(error, args))
  def appendForm(error: String, args: Any*): Outcome[T] = appendForm(ValidationError(error, args))
  def appendField(field: String, error: String, args: Any*): Outcome[T] = appendField(field, ValidationError(error, args))

  def appendGlobal(error: ValidationError): Outcome[T] = copy(global = global :+ error)
  def appendForm(error: ValidationError): Outcome[T] = copy(form = form :+ error)
  def appendField(field: String, error: ValidationError): Outcome[T] = copy(fields = addField(fields, field, error))

  def appendFields(errors: Seq[(String, Seq[ValidationError])]): Outcome[T] = copy(fields = {
    if (fields.isEmpty) {
      errors.toMap
    } else {
      var tmpFields = fields;
      errors.foreach { error =>
        tmpFields = addField(tmpFields, error._1, error._2)
      }
      tmpFields
    }
  })

  def get: T = if (isSuccess) value.get else throw new Exception("")

  def to[U]: Outcome[U] = this.copy(value = None).asInstanceOf[Outcome[U]]

  def map[U](f: T => U): Outcome[U] = copy(value = value.map(f))

  def asyncMap[U](f: T => Future[U])(implicit ec: ExecutionContext): Future[Outcome[U]] =
    if (value.isDefined) {
      f(value.get).map(v => this.map(_ => v))(ec)
    } else {
      Future.successful(this.to[U])
    }

  def check[U](f: (T, Outcome[T])=> Outcome[U]): Outcome[U] =
    if (value.isDefined) f(value.get, this)
    else this.asInstanceOf[Outcome[U]]

  def asyncCheck[U](f: (T, Outcome[T])=> Future[Outcome[U]]): Future[Outcome[U]] =
    if (value.isDefined) f(value.get, this)
    else Future.successful(this.asInstanceOf[Outcome[U]])

  // def check(f: (T, Outcome[T])=> Outcome[T]): Outcome[T] = if (value.isDefined) f(value.get, this) else this

  def successStatus(status: Int): Outcome[T] = this.copy(successStatus = Some(status))
  def failureStatus(status: Int): Outcome[T] = this.copy(failureStatus = Some(status))
  def status(status: Int): Outcome[T] = if (isSuccess) successStatus(status) else failureStatus(status)

  private def getSuccessStatus(orElse: Int): Int = successStatus.getOrElse(orElse)
  private def getFailureStatus(orElse: Int): Int = failureStatus.getOrElse(orElse)

  // JSON API
  // def toResult(implicit writer: Writes[T]): Result
  // def toResult(messagesApi: MessagesApi)(implicit writer: Writes[T], lang: Lang): Result

  // Validation API
  def toResult(implicit writer: Write[T, JsValue]): Result = if (isFailure) {
    getErrorResponse()
  } else {
    new Results.Status(getSuccessStatus(Status.OK))(writer.writes(value.get))
  }

  def toResult(messagesApi: MessagesApi)(implicit writer: Write[T, JsValue], lang: Lang): Result = if (isFailure) {
    getErrorResponse(messagesApi, lang)
  } else {
    new Results.Status(getSuccessStatus(Status.OK))(writer.writes(value.get))
  }

  // Message keys
  private def veToString(error: ValidationError): String = error.messages.headOption.getOrElse("")
  private def vesToString(errors: Seq[ValidationError]): Seq[String] = errors.map(veToString)
  private def vesToJsArray(errors: Seq[ValidationError]): JsArray = new JsArray(vesToString(errors).map(JsString.apply))

  private def getErrors(): JsValue = Json.obj(
    "global" -> vesToJsArray(global),
    "form" -> vesToJsArray(form),
    "fields" -> JsObject(fields.mapValues(vesToJsArray).toSeq)
  )

  // I18N support
  private def veToString(error: ValidationError, msg: MessagesApi, lang: Lang): String =
    msg(error.messages, error.args: _*)

  private def vesToString(errors: Seq[ValidationError], msg: MessagesApi, lang: Lang): Seq[String] =
    errors.map(e => veToString(e, msg, lang))

  private def vesToJsArray(errors: Seq[ValidationError], msg: MessagesApi, lang: Lang): JsArray =
    new JsArray(vesToString(errors, msg, lang).map(JsString.apply))

  private def getErrors(msg: MessagesApi, lang: Lang): JsValue = Json.obj(
    "global" -> vesToJsArray(global, msg, lang),
    "form" -> vesToJsArray(form, msg, lang),
    "fields" -> JsObject(fields.mapValues(e => vesToJsArray(e, msg, lang)).toSeq)
  )

  private def getErrorResponse() =
    if (hasErrors) {
      new Results.Status(getFailureStatus(Status.BAD_REQUEST))(getErrors())
    } else {
      new Results.Status(getFailureStatus(Status.NOT_FOUND))
    }

  private def getErrorResponse(messagesApi: MessagesApi, lang: Lang) =
    if (hasErrors) {
      new Results.Status(getFailureStatus(Status.BAD_REQUEST))(getErrors(messagesApi, lang))
    } else {
      new Results.Status(getFailureStatus(Status.NOT_FOUND))
    }
}

object Outcome {
  def successful[T](value: T): Outcome[T] = Outcome(value = Some(value))
  def failed[T]: Outcome[T] = Outcome()

  def fromOption[T](value: Option[T]): Outcome[T] = Outcome(value = value)

  def fromJson[T](value: JsValue)(implicit rule: Rule[JsValue, T]): Outcome[T] = rule.validate(value).fold (
    errors => failed.appendFields(errors.map(kv => kv._1.toString -> kv._2)),
    item => successful(item)
  )

  def fromJsResult[T](jsValue: JsResult[T]): Outcome[T] = jsValue match {
    case JsSuccess(value, path) => successful(value)
    case JsError(errors) => failed.appendFields(errors.map(kv => kv._1.toString -> kv._2))
  }

  def read[T](value: JsValue)(implicit reader: Reads[T]): Outcome[T] = Outcome.fromJsResult(reader.reads(value))
}
