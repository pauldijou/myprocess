package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

class Application (val actions: Actions) extends Api {

  def emptyIndex() = index("")

  def index(url: String) = Action {
    Ok(views.html.index())
  }

  def authenticated() = Authenticated {
    Ok("")
  }

  def fail() = Action {
    BadRequest(Json.obj("msg" -> "failure", "args" -> 1))
  }

  def error() = Action {
    throw new RuntimeException("YOU FAIL !")
    BadRequest(Json.obj("msg" -> "failure", "args" -> 1))
  }

}
