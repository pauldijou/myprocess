// package outcome
//
// import scala.concurrent.Future
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.util.{Try, Success => TrySuccess, Failure => TryFailure}
// import play.api.mvc.{Result, Results}
// import play.api.http.Status
// import play.api.libs.json._
// import play.api.data.validation.ValidationError
// import play.api.i18n.{MessagesApi, Lang}
//
// import play.api.data.mapping._
//
// sealed trait Outcome[T] {
//   def isSuccess(): Boolean
//   def isFailure(): Boolean
//
//   def appendGlobal(error: String, args: Any*): Outcome[T]
//   def appendForm(error: String, args: Any*): Outcome[T]
//   def appendField(field: String, error: String, args: Any*): Outcome[T]
//
//   def appendGlobal(error: ValidationError): Outcome[T]
//   def appendForm(error: ValidationError): Outcome[T]
//   def appendField(field: String, error: ValidationError): Outcome[T]
//
//   def appendFields(errors: Seq[(String, Seq[ValidationError])]): Outcome[T]
//
//   def filter(p: (T) ⇒ Boolean): Outcome[T]
//   def flatMap[U](f: (T) ⇒ Outcome[U]): Outcome[U]
//   def foreach[U](f: (T) ⇒ U): Unit
//   def get: T
//   def map[U](f: (T) ⇒ U): Outcome[U]
//   // def getOrElse(default: => T): T = if (isSuccess) get else default
//   // def orElse(default: => Outcome[T]): Outcome[T] = if (isSuccess) this else default
//   def toOption: Option[T] = if (isSuccess) Some(get) else None
//   def toTry: Try[T]
//
//   def status(): Option[Int]
//   def status(status: Int): Outcome[T]
//
//   protected def getStatus(orElse: Int): Int = status.getOrElse(orElse)
//
//   // JSON API
//   // def toResult(implicit writer: Writes[T]): Result
//   // def toResult(messagesApi: MessagesApi)(implicit writer: Writes[T], lang: Lang): Result
//
//   // Validation API
//   def toResult(implicit writer: Write[T, JsObject]): Result
//   def toResult(messagesApi: MessagesApi)(implicit writer: Write[T, JsObject], lang: Lang): Result
// }
//
// object Outcome {
//   def successful[T](value: T): Outcome[T] = Success(Future.successful(value))
//   def successful[T](value: T): Outcome[T] = Success(value)
//   def failed[T]: Outcome[T] = Failure()
//
//   def fromOption[T](value: Option[T]): Outcome[T] = value.map(v => successful(v)).getOrElse(failed)
//
//   def fromJson[T](value: JsValue)(implicit rule: Rule[JsValue, T]): Outcome[T] = rule.validate(value).fold (
//     errors => failed.appendFields(errors.map(kv => kv._1.toString -> kv._2)),
//     item => successful(item)
//   )
//
//   def fromJsResult[T](jsValue: JsResult[T]): Outcome[T] = jsValue match {
//     case JsSuccess(value, path) => successful(value)
//     case JsError(errors) => failed.appendFields(errors.map(kv => kv._1.toString -> kv._2))
//   }
//
//   def read[T](value: JsValue)(implicit reader: Reads[T]): Outcome[T] = Outcome.fromJsResult(reader.reads(value))
// }
//
// case class Success[T](value: T, status: Option[Int] = None) extends Outcome[T] {
//   def isSuccess() = true
//   def isFailure() = false
//
//   def value[U](v: U): Outcome[U] = copy(value = Future.successful(v))
//
//   def appendGlobal(error: String, args: Any*): Outcome[T] = Outcome.failed.appendGlobal(error, args)
//   def appendForm(error: String, args: Any*): Outcome[T] = Outcome.failed.appendForm(error, args)
//   def appendField(field: String, error: String, args: Any*): Outcome[T] = Outcome.failed.appendField(field, error, args)
//
//   def appendGlobal(error: ValidationError): Outcome[T] = Outcome.failed.appendGlobal(error)
//   def appendForm(error: ValidationError): Outcome[T] = Outcome.failed.appendForm(error)
//   def appendField(field: String, error: ValidationError): Outcome[T] = Outcome.failed.appendField(field, error)
//
//   def appendFields(errors: Seq[(String, Seq[ValidationError])]): Outcome[T] = Outcome.failed.appendFields(errors)
//
//   def status(status: Int): Outcome[T] = copy(status = Some(status))
//
//   def get: T = value
//   def flatMap[U](f: (T) ⇒ Outcome[U]): Outcome[U] = f(value)
//   def foreach[U](f: T => U): Unit = f(value)
//   def map[U](f: T => U): Outcome[U] = Outcome.successful(f(value))
//   def filter(p: T => Boolean): Outcome[T] = if(p(value)) this else Outcome.failed
//   def toTry: Try[T] = TrySuccess(value)
//
//   // def toResult(implicit writer: Writes[T]): Result = {
//   //   new Results.Status(getStatus(Status.OK))(writer.writes(value))
//   // }
//   //
//   // def toResult(messagesApi: MessagesApi)(implicit writer: Writes[T], lang: Lang): Result = {
//   //   new Results.Status(getStatus(Status.OK))(writer.writes(value))
//   // }
//
//   def toResult(implicit writer: Write[T, JsObject]): Result =
//     new Results.Status(getStatus(Status.OK))(writer.writes(value))
//
//   def toResult(messagesApi: MessagesApi)(implicit writer: Write[T, JsObject], lang: Lang): Result =
//     new Results.Status(getStatus(Status.OK))(writer.writes(value))
// }
//
// case class Failure[T](
//   global: Seq[ValidationError] = Seq(),
//   form: Seq[ValidationError] = Seq(),
//   fields: Map[String, Seq[ValidationError]] = Map(),
//   status: Option[Int] = None
// ) extends Outcome[T] {
//   def isSuccess() = false
//   def isFailure() = true
//
//   def value[U](v: U): Outcome[U] = if (hasErrors) this.asInstanceOf[Outcome[U]] else Outcome.successful(v)
//
//   private def addField(fields: Map[String, Seq[ValidationError]], field: String, error: ValidationError) =
//     fields + (field -> fields.get(field).map(_ :+ error).getOrElse(Seq(error)))
//
//   private def addField(fields: Map[String, Seq[ValidationError]], field: String, errors: Seq[ValidationError]) =
//     fields + (field -> fields.get(field).map(_ ++ errors).getOrElse(errors))
//
//   def appendGlobal(error: String, args: Any*): Outcome[T] = appendGlobal(ValidationError(error, args))
//   def appendForm(error: String, args: Any*): Outcome[T] = appendForm(ValidationError(error, args))
//   def appendField(field: String, error: String, args: Any*): Outcome[T] = appendField(field, ValidationError(error, args))
//
//   def appendGlobal(error: ValidationError): Outcome[T] = copy(global = global :+ error)
//   def appendForm(error: ValidationError): Outcome[T] = copy(form = form :+ error)
//   def appendField(field: String, error: ValidationError): Outcome[T] = copy(fields = addField(fields, field, error))
//
//   def appendFields(errors: Seq[(String, Seq[ValidationError])]): Outcome[T] = copy(fields = {
//     if (fields.isEmpty) {
//       errors.toMap
//     } else {
//       var tmpFields = fields;
//       errors.foreach { error =>
//         tmpFields = addField(tmpFields, error._1, error._2)
//       }
//       tmpFields
//     }
//   })
//
//   def status(status: Int): Outcome[T] = copy(status = Some(status))
//
//   def get: T = throw new RuntimeException("") // FIXME
//   def flatMap[U](f: (T) ⇒ Outcome[U]): Outcome[U] = this.asInstanceOf[Outcome[U]]
//   def foreach[U](f: T => U): Unit = ()
//   def map[U](f: T => U): Outcome[U] = this.asInstanceOf[Outcome[U]]
//   def filter(p: T => Boolean): Outcome[T] = this
//   def toTry: Try[T] = TryFailure(new Exception("getErrors()")) // FIXME
//
//   private def hasErrors(): Boolean = !global.isEmpty || !form.isEmpty || !fields.isEmpty
//
//   // Message keys
//   private def veToString(error: ValidationError): String = error.messages.headOption.getOrElse("")
//   private def vesToString(errors: Seq[ValidationError]): Seq[String] = errors.map(veToString)
//   private def vesToJsArray(errors: Seq[ValidationError]): JsArray = new JsArray(vesToString(errors).map(JsString.apply))
//
//   private def getErrors(): JsValue = Json.obj(
//     "global" -> vesToJsArray(global),
//     "form" -> vesToJsArray(form),
//     "fields" -> JsObject(fields.mapValues(vesToJsArray).toSeq)
//   )
//
//   // I18N support
//   private def veToString(error: ValidationError, msg: MessagesApi, lang: Lang): String =
//     msg(error.messages, error.args: _*)
//
//   private def vesToString(errors: Seq[ValidationError], msg: MessagesApi, lang: Lang): Seq[String] =
//     errors.map(e => veToString(e, msg, lang))
//
//   private def vesToJsArray(errors: Seq[ValidationError], msg: MessagesApi, lang: Lang): JsArray =
//     new JsArray(vesToString(errors, msg, lang).map(JsString.apply))
//
//   private def getErrors(msg: MessagesApi, lang: Lang): JsValue = Json.obj(
//     "global" -> vesToJsArray(global, msg, lang),
//     "form" -> vesToJsArray(form, msg, lang),
//     "fields" -> JsObject(fields.mapValues(e => vesToJsArray(e, msg, lang)).toSeq)
//   )
//
//   private def getResponse() =
//     if (hasErrors) {
//       new Results.Status(getStatus(Status.BAD_REQUEST))(getErrors())
//     } else {
//       new Results.Status(getStatus(Status.NOT_FOUND))
//     }
//
//   private def getResponse(messagesApi: MessagesApi, lang: Lang) =
//     if (hasErrors) {
//       new Results.Status(getStatus(Status.BAD_REQUEST))(getErrors(messagesApi, lang))
//     } else {
//       new Results.Status(getStatus(Status.NOT_FOUND))
//     }
//
//   // def toResult(implicit writer: Writes[T]): Result = getResponse()
//   //
//   // def toResult(messagesApi: MessagesApi)(implicit writer: Writes[T], lang: Lang): Result = {
//   //   if (hasErrors) {
//   //     new Results.Status(getStatus(Status.BAD_REQUEST))(getErrors(messagesApi, lang))
//   //   } else {
//   //     new Results.Status(getStatus(Status.NOT_FOUND))
//   //   }
//   // }
//
//   def toResult(implicit writer: Write[T, JsObject]): Result = getResponse()
//
//   def toResult(messagesApi: MessagesApi)(implicit writer: Write[T, JsObject], lang: Lang): Result =
//     getResponse(messagesApi, lang)
// }
