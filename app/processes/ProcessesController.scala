package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import models.Process
import services.Processes
import outcome.Outcome

class ProcessesController (val actions: Actions, processes: Processes) extends Controller {
  def create() = Action(parse.json) { request =>
    Outcome.fromJson[Process](request.body).toResult
  }
}
