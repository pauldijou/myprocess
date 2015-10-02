package bootstrap

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import play.api.http.ContentTypes
import play.api.libs.json._

class ErrorHandler extends HttpErrorHandler {
  private def want(request: RequestHeader, contentType: String): Boolean = {
    request.contentType.map(_ == contentType).getOrElse(false)
  }

  private def wantJSON(request: RequestHeader): Boolean = {
    want(request, ContentTypes.JSON)
  }

  private def throwableToJson(t: Throwable): JsObject = {
    val cause: JsValue = if (t.getCause() == null) { JsNull } else { throwableToJson(t.getCause()) }
    Json.obj(
      "message" -> t.getMessage,
      "stack" -> t.getStackTrace.map(_.toString),
      "cause" -> cause
    )
  }

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    println("onClientError " + message)
    // FIXME
    Future.successful(
      if (wantJSON(request)) {
        new Status(statusCode)(Json.obj("error" -> true, "message" -> message)).as(ContentTypes.JSON)
      } else {
        // FIXME: actually display an error page
        new Status(statusCode)
      }
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    println("onServerError")
    // FIXME: log error
    // exception.printStackTrace()
    Future.successful(
      if (wantJSON(request)) {
        InternalServerError("").as(ContentTypes.JSON)
      } else {
        // FIXME: actually display an error page
        InternalServerError("")
      }
    )
  }
}
