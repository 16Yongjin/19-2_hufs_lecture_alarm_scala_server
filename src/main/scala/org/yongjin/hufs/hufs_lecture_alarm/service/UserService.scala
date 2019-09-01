package org.yongjin.hufs.hufs_lecture_alarm.service

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.functor._
import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.UserRepository

class UserService[F[_]: Async](userRepo: UserRepository[F]) {

  def listLectures(courseId: String): EitherT[F, ApiError, List[Lecture]] = {
    userRepo.listLectures(courseId).toRight[ApiError](UnknownError)
  }

  def addUserAlarm(createAlarm: CreateAlarm): EitherT[F, ApiError, List[Lecture]] = {

    userRepo.addUserAlarm(createAlarm).toRight[ApiError](UserNotFound(createAlarm.user))
  }

  def removeUserAlarm(user: User, lectureId: String): EitherT[F, ApiError, List[Lecture]] = {
    userRepo.removeUserAlarm(user, lectureId).toRight[ApiError](UnknownError)
  }

  def findUserAlarms(user: User): EitherT[F, ApiError, List[Lecture]] = {
    userRepo.findUserAlarms(user).toRight[ApiError](UserNotFound(user))
  }

}
