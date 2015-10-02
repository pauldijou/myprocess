package bootstrap

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import db.Driver
import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import router.Routes
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import play.api.db.slick.{SlickComponents, DbName}
import org.mindrot.jbcrypt.BCrypt

import models._

class Loader extends ApplicationLoader {
  def load(context: Context) = {
    new Components(context).application
  }
}

class Components(context: Context) extends BuiltInComponentsFromContext(context) with SlickComponents {
  import Driver.api._

  // DB
  val dbConf = api.dbConfig[JdbcProfile](DbName("default"))
  val db = dbConf.db

  // HTTP error handler
  val errorHandler = new ErrorHandler()

  // Services
  val roles = new services.Roles(dbConf)
  val users = new services.Users(dbConf)
  val processes = new services.Processes(dbConf)

  // Actions
  val actions = new controllers.Actions(users, roles)

  // Controllers
  val applicationController = new controllers.Application(actions)
  val assets = new controllers.Assets(httpErrorHandler)
  // val rolesController = new controllers.RolesController(actions, roles)
  val usersController = new controllers.UsersController(actions, users, roles, configuration)
  val processesController = new controllers.ProcessesController(actions, processes)


  // Final router
  val router = new Routes(errorHandler, applicationController, assets, usersController)

  // Some utils
  def interpolateSQL(query: String) = sqlu"#$query"

  val schema = {
    services.Roles.schema ++
    services.Users.schema ++
    services.Processes.schema
  }

  // onStop
  applicationLifecycle.addStopHook { () =>
    println("STOP")

    db.shutdown.map { _ =>
      println("DB shutdown")
      ()
    }
  }

  try {
    // onStart
    println("START")

    def getTables() = {
      Await.result(db.run(MTable.getTables), Duration.Inf)
    }

    def hasTable(tables: Vector[MTable], stmt: String): Boolean = tables.exists { table =>
      stmt.toLowerCase.contains(s"""table "${table.name.name.toLowerCase}""")
    }

    println("-----------------------------------------------");
    println("Dropping tables...")
    println("")

    val preTables: Vector[MTable] = getTables()

    schema.dropStatements.foreach { stmt =>
      if (hasTable(preTables, stmt)) {
        val dropQuery = interpolateSQL(stmt)
        dropQuery.statements.foreach(println)
        Await.result(db.run(dropQuery), Duration.Inf)
        println("")
      }
    }

    println("All tables dropped")
    println("-----------------------------------------------");
    println("Creating tables...")
    println("")

    val postTables: Vector[MTable] = getTables()

    schema.createStatements.foreach { stmt =>
      if (!hasTable(postTables, stmt)) {
        val createQuery = interpolateSQL(stmt)
        createQuery.statements.foreach(println)
        Await.result(db.run(createQuery), Duration.Inf)
        println("")
      }
    }

    println("All tables created")
    println("-----------------------------------------------");
    println("Inserting default data...")
    println("")

    val password = BCrypt.hashpw("password", BCrypt.gensalt(configuration.getInt("play.crypto.factor").getOrElse(10)))

    val uuid = java.util.UUID.fromString("9181fd4e-1f36-449d-9e20-679e4da813f9")
    val uuid2 = java.util.UUID.fromString("6e4a48c0-a132-4daf-8332-15e893bdbe48")

    val adminReference = Reference(uuid, uuid)

    val adminRole = Role(Reference(uuid2, adminReference.id), "admin", Seq(Permission.ADMIN))
    val userRole = Role(Reference.by(adminReference), "user", Seq())

    val paul = User(adminReference, "Paul", "paul.dijou@gmail.com", password, adminRole.id)
    val didier = User(Reference.random(), "Didier", "paul.dijou+alt@gmail.com", password, userRole.id)

    List(adminRole, userRole).map { role =>
      Await.result(roles.insert(role), Duration.Inf)
    }

    List(paul, didier).map { user =>
      Await.result(users.insert(user), Duration.Inf)
    }

    println("Data created")
    println("-----------------------------------------------");
  } catch {
    case t: Throwable => {
      // We don't really want to crash everything since it would prevent the onStop hook to perform
      t.printStackTrace()
    }
  }
}
