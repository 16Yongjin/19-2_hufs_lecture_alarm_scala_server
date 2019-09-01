package org.yongjin.hufs.hufs_lecture_alarm.http

import cats.Monad
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.yongjin.hufs.hufs_lecture_alarm.model._

class HttpErrorHandler[F[_]: Monad] extends Http4sDsl[F] {

  // Map your business errors to responses here
  val handle: ApiError => F[Response[F]] = {
    case UserNotFound(u)     => NotFound(s"User not found ${u}")
    case LectureNotFound(l)  => NotFound(s"Lecture not found")
    case UnknownError        => InternalServerError("internal server error")
    case LectureParsingError => InternalServerError("강의 목록을 가져오는 데 실패했습니다.")
    case AlarmBatchError     => InternalServerError("알람 배치 에러")
  }

}
