package org.yongjin.hufs.hufs_lecture_alarm

import cats.data.ValidatedNel
import cats.syntax.apply._
import cats.syntax.validated._
import org.yongjin.hufs.hufs_lecture_alarm.model._
import cats.Applicative

object validation {

  object UserValidation {
    type Result[A] = ValidatedNel[String, A]

    private def validateUsername(username: String): Result[UserName] = {
      if (username.matches("^[a-zA-Z0-9]+$")) new UserName(username).validNel[String]
      else "Invalid username".invalidNel
    }

    private def validateEmail(email: String): Result[Email] = {
      if ("""(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined) new Email(email).validNel[String]
      else "Invalid email".invalidNel
    }

    private def validateUser(user: User): Result[User] = {
      if (user.id.nonEmpty) return user.validNel[String]
      else "Invalid user id".invalidNel
    }

    def validateUpdateUser(updateUser: UpdateUser): Result[Email] = {
      validateEmail(updateUser.email)
    }

  }

  object AlarmValidation {
    type Result[A] = ValidatedNel[String, A]

    private def validateUserId(user: User): Result[User] = {
      if (!user.id.isEmpty) user.validNel[String]
      else "알람을 보낼 수 없는 유저입니다.".invalidNel
    }

    private def validateLectureId(lectureId: String): Result[String] = {
      if (lectureId.isEmpty) "강의 아이디가 없습니다.".invalidNel
      else lectureId.validNel[String]
    }

    def validateAlarm(createAlarm: CreateAlarm): Result[CreateAlarm] = {
      (validateUserId(createAlarm.user), validateLectureId(createAlarm.lectureId))
        .mapN(CreateAlarm.apply)
    }

  }

}
