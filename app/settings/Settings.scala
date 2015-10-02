// package models
//
// import play.api.libs.json.JsValue
// import play.api.libs.functional.syntax._
// import play.api.data.mapping._
//
// abstract class Settings[T] (
//   reference: Reference,
//   model: String,
//   data: JsValue
// ) extends Model {
//   def value(implicit rule: Rule[JsValue, T]): Option[T] = rule.validate(data).asOpt
// }
//
// trait Settings[T] extends Model {
//   def value(): T
//
// }
//
// object Settings {
//   case class DefaultRole (
//     reference: Reference,
//     value: Role
//   ) extends Settings[Role]
// }
