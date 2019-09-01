package org.yongjin.hufs.hufs_lecture_alarm.http

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.service.UserService
import org.yongjin.hufs.hufs_lecture_alarm.validation._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UserHttpRoutes[F[_]: Sync](userService: UserService[F])(implicit H: HttpErrorHandler[F])
    extends Http4sDsl[F] {

  implicit def createUserDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // 등록된 알람 목록
    case GET -> Root / "myalarm" / userId =>
      for {
        alarms   <- userService.findUserAlarms(User(userId)).value
        response <- alarms.fold(H.handle, x => Ok(x.asJson))
      } yield response

    // 알람 삭제
    case DELETE -> Root / "myalarm" / userId / lectureId =>
      for {
        users    <- userService.removeUserAlarm(User(userId), lectureId).value
        response <- users.fold(H.handle, x => Ok(x.asJson))
      } yield response

    // 알람 추가
    case req @ POST -> Root / "myalarm" =>
      req.decode[CreateAlarm] { createAlarm =>
        AlarmValidation
          .validateAlarm(createAlarm)
          .fold(
            errors => BadRequest(errors.toList.asJson),
            createAlarm =>
              userService.addUserAlarm(createAlarm).value flatMap { either =>
                either.fold(H.handle, x => Created(x.asJson))
              }
          )
      }

    // 강의 목록 보기
    case GET -> Root / "lectures" / CourseIdVar(courseId) =>
      for {
        lectures <- userService.listLectures(courseId).value
        response <- lectures.fold(H.handle, x => Ok(x.asJson))
      } yield response

  }

  object CourseIdVar {
    val courseIdRegex = """(.+_H[12])""".r
    def unapply(str: String): Option[String] = {
      str match {
        case courseIdRegex(s) => Some(s)
        case _                => None
      }
    }
  }

}
