package org.yongjin.hufs.hufs_lecture_alarm.repository

import org.yongjin.hufs.hufs_lecture_alarm.model._
import cats.data.OptionT

object algebra {

  trait UserRepository[F[_]] {

    def addUserAlarm(createAlarm: CreateAlarm): OptionT[F, List[Lecture]]

    def removeUserAlarm(user: User, lectureId: String): OptionT[F, List[Lecture]]

    def findUserAlarms(user: User): OptionT[F, List[Lecture]]

    def listLectures(courseId: String): OptionT[F, List[Lecture]]

  }

  trait AlarmRepository[F[_]] {

    def findAlarmToAlert(lectureId: String): OptionT[F, Alarm]

    def findAllAlarmToAlert(lectureIds: List[String]): OptionT[F, List[Alarm]]

    def findAlarmLectures(): OptionT[F, List[Lecture]]

    def findAllUserToAlert(lectureId: String): OptionT[F, List[User]]

    def removeAlertedAlarm(lectureId: String): OptionT[F, Unit]

    def removeAlertedAlarms(lectureIds: List[String]): OptionT[F, List[Unit]]

    def addLectures(lectures: List[Lecture]): OptionT[F, String]

  }

}
