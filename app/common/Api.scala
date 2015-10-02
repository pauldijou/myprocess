package controllers

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import pdi.jwt._

import models.{User, Role}
import models.Permission._
import services.{ Users, Roles }

trait Api extends Controller {
  def actions: Actions

  def Default = Action
  def Authenticated = actions.Authenticated
  def Can(permissions: Permission*) = actions.Can _
}

case class UserToken(id: UUID)
object UserToken {
  implicit val tokenReads = Json.reads[UserToken]
  implicit val tokenWrite = Json.writes[UserToken]
}

case class UserSession(user: User, role: Role)

class AuthenticatedRequest[A](val jwtSession: UserSession, request: Request[A]) extends WrappedRequest[A](request)

class Actions (users: Users, roles: Roles) {
  def Authenticated = AuthenticatedAction
  def Can(permissions: Permission*) = new CanAction(permissions.toSeq)

  private def getSession(userId: UUID): Future[Option[UserSession]] = users.findById(userId).flatMap {
    case Some(user) => roles.findById(user.role).map { _.map { role =>
      UserSession(user, role)
    }}
    case None => Future.successful(None)
  }

  object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) =  {
      request.jwtSession.getAs[UserToken]("user")(UserToken.tokenReads) match {
        case Some(token) => getSession(token.id).flatMap {
          case Some(session) => block(new AuthenticatedRequest(session, request)).map(_.refreshJwtSession(request))
          case _ => Future.successful(Unauthorized)
        }
        case _ => Future.successful(Unauthorized)
      }
    }
  }

  class CanAction(permissions: Seq[Permission]) extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) =  {
      request.jwtSession.getAs[UserToken]("user")(UserToken.tokenReads) match {
        case Some(token) => getSession(token.id).flatMap {
          case Some(session) => if (session.role.can(permissions)) {
            block(new AuthenticatedRequest(session, request)).map(_.refreshJwtSession(request))
          } else {
            Future.successful(Forbidden)
          }
          case _ => Future.successful(Unauthorized)
        }
        case _ => Future.successful(Unauthorized)
      }
    }
  }
}
