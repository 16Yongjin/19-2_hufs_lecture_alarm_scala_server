package org.yongjin.hufs.hufs_lecture_alarm.http

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.service.UserService
import org.yongjin.hufs.hufs_lecture_alarm.service.AlarmService
import org.yongjin.hufs.hufs_lecture_alarm.validation.UserValidation
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class AlarmHttpRoutes[F[_]: Sync](alarmService: AlarmService[F])(implicit H: HttpErrorHandler[F])
    extends Http4sDsl[F] {

  implicit def createUserDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // 0 ~ 15까지 숫자를 받아서 강의 목록 추가함
    case GET -> Root / "addLectures" / IntVar(index) =>
      for {
        courses  <- alarmService.addLecturesBatch(index).value
        response <- courses.fold(H.handle, x => Ok(x))
      } yield response

    case GET -> Root / "batch" =>
      for {
        courses  <- alarmService.alarmBatch.value
        response <- courses.fold(H.handle, _ => Ok())
      } yield response

  }

}
