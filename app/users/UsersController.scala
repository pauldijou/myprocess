package controllers

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Configuration
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt._

import models.{ Reference, User}
import services.{ Users, Roles }
import outcome.Outcome

class UsersController (val actions: Actions, users: Users, roles: Roles, configuration: Configuration) extends Api {
  // Login
  case class LoginBody(email: String, password: String)

  val parseLogin = From[JsValue] { __ =>
    import play.api.data.mapping.json.Rules._
    (
      (__ \ "email").read[String] ~
      (__ \ "password").read[String]
    )(LoginBody.apply _)
  }

  def login() = Default.async(parse.json) { implicit request =>
    Outcome.fromJson(request.body)(parseLogin).asyncCheck { (body, outcome)=>
      users.findByEmail(body.email).map {
        case Some(user) if BCrypt.checkpw(body.password, user.password) => outcome.map(_ => user)
        case _ => outcome.to[User].appendForm("Invalid credentials")
      }
    }.map { out =>
      if (out.isSuccess) {
        out.toResult.addingToJwtSession("user", UserToken(out.get.id))
      } else {
        out.toResult
      }
    }
  }

  // Register
  case class RegisterBody(name: String, email: String, password: String)

  val parseRegister = From[JsValue] { __ =>
    import play.api.data.mapping.json.Rules._
    (
      (__ \ "name").read[String] ~
      (__ \ "email").read[String] ~
      (__ \ "password").read[String]
    )(RegisterBody.apply _)
  }

  def register() = Default.async(parse.json) { request =>
    Outcome.fromJson(request.body)(parseRegister).asyncCheck { (body, outcome)=>
      users.findByEmail(body.email).map {
        case Some(_) => outcome.appendForm("Email already used")
        case None => outcome
      }
    }.flatMap { _.asyncMap { body =>
        roles.findByName("user").flatMap { role =>
          val ref = Reference.random()
          val password = BCrypt.hashpw(body.password, BCrypt.gensalt(configuration.getInt("play.crypto.factor").getOrElse(10)))
          val user = User(ref, body.name, body.email, password, role.get.id)
          users.insert(user).map(_ => user)
        }
      }
    }.map(_.toResult)
  }

  def all() = Authenticated.async { request =>
    import play.api.data.mapping.json.Writes._
    users.all().map { u =>
      Outcome.successful(u).toResult
    }
  }

  def byId(id: UUID) = Authenticated.async { request =>
    import play.api.data.mapping.json.Writes._
    users.findById(id).map { u =>
      Outcome.fromOption(u).toResult
    }
  }

  def current() = Authenticated { request =>
    Outcome.successful(request.jwtSession.user).toResult
  }
}
