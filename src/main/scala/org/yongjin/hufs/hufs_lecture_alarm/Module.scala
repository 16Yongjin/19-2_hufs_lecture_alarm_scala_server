package org.yongjin.hufs.hufs_lecture_alarm
import cats.temp.par._

import cats.Parallel
import cats.effect.{Async, ContextShift}

import doobie.util.transactor.Transactor

import org.http4s.server.Router
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.yongjin.hufs.hufs_lecture_alarm.http.{HttpErrorHandler, UserHttpRoutes, AlarmHttpRoutes}
import org.yongjin.hufs.hufs_lecture_alarm.repository.{
  PostgresUserRepository,
  PostgresAlarmRepository
}
import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.{UserRepository, AlarmRepository}
import org.yongjin.hufs.hufs_lecture_alarm.service.{UserService, AlarmService}
import org.yongjin.hufs.hufs_lecture_alarm.scraper.HttpClient
import org.http4s.server.middleware.CORS

import scala.util.Properties

// Custom DI module
class Module[F[_]: Async: ContextShift: Parallel](client: HttpClient[F]) {

  private val xa: Transactor[F] =
    Transactor.fromDriverManager[F](
      "org.postgresql.Driver",
      Properties.envOrElse("POSTGRES_DB", "jdbc:postgresql:lectures"),
      Properties.envOrElse("POSTGRES_USER", "postgres"),
      Properties.envOrElse("POSTGRES_PASSWARD", "postgres")
    )

  private val userRepository: UserRepository[F] = new PostgresUserRepository[F](xa)

  private val userService: UserService[F] = new UserService[F](userRepository)

  implicit val httpErrorHandler: HttpErrorHandler[F] = new HttpErrorHandler[F]

  private val userRoutes: HttpRoutes[F] = new UserHttpRoutes[F](userService).routes

  private val alarmRepository: AlarmRepository[F] = new PostgresAlarmRepository[F](xa)

  val alarmService: AlarmService[F] = new AlarmService[F](alarmRepository)(client)

  // For Tesing Purpose
  // Should Delete When deployed
  private val alarmRoutes: HttpRoutes[F] = new AlarmHttpRoutes[F](alarmService).routes

  private val httpRoutes: HttpRoutes[F] = Router[F](
    ""       -> userRoutes,
    "/alarm" -> alarmRoutes
  )

  val httpApp: HttpApp[F] = CORS(httpRoutes.orNotFound)

}
