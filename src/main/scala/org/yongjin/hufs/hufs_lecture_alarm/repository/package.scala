package org.yongjin.hufs.hufs_lecture_alarm

import org.yongjin.hufs.hufs_lecture_alarm.model._
import doobie.postgres.implicits._

package object repository {

  type UserDTO = String

  implicit class UserConversions(dto: UserDTO) {
    def toUser: User = User(
      id = dto
    )
  }

  type AlarmDTO = (List[String], String, String, Int, String, String, String)

  implicit class AlarmConversions(dto: AlarmDTO) {
    def toAlarm: Alarm = Alarm(
      users = dto._1.map(User),
      lecture = Lecture(dto._2, dto._3, dto._4, dto._5, dto._6, dto._7)
    )
  }

  type LectureDTO = (String, String, Int, String, String, String)

  implicit class LectureConversions(dto: LectureDTO) {
    def toLecture: Lecture = Lecture(dto._1, dto._2, dto._3, dto._4, dto._5, dto._6)
  }

}
