package org.yongjin.hufs.hufs_lecture_alarm

object model {

  case class Email(value: String)    extends AnyVal
  case class UserName(value: String) extends AnyVal

  // case class User(username: UserName, email: Email)

  case class User(id: String)

  // Lecture

  case class Lecture(
      id: String,
      course: String,
      index: Int,
      name: String,
      professor: String,
      time: String
  )

  case class LectureWithEmptyCheck(
      id: String,
      course: String,
      index: Int,
      name: String,
      professor: String,
      time: String,
      isEmpty: Boolean
  )

  implicit class LectureConversions(lectures: List[Lecture]) {
    def toCourseIndexMap: List[(String, List[Int])] =
      lectures.map(l => (l.course, l.index)).groupBy(_._1).mapValues(_.map(_._2)).toList
  }

  implicit class LectureWithEmptyCheckConversions(dto: LectureWithEmptyCheck) {
    def toLecture: Lecture =
      Lecture(dto.id, dto.course, dto.index, dto.name, dto.professor, dto.time)
  }

  case class Alarm(users: List[User], lecture: Lecture)

  case class Notification(title: String, icon: String)
  case class AlarmMessage(registration_ids: List[String], notification: Notification)

  object AlarmMessage {

    def apply(alarm: Alarm): AlarmMessage = new AlarmMessage(
      alarm.users.map(_.id),
      buildNotification(alarm.lecture)
    )

    private def buildNotification(lecture: Lecture): Notification = {
      val msg  = s"""${lecture.name} / ${lecture.professor} / ${lecture.time} 자리 났습니다!!"""
      val icon = "noti-icon.png"

      Notification(msg, icon)
    }

  }

  // Business errors
  sealed trait ApiError                         extends Product with Serializable
  case class UserNotFound(user: User)           extends ApiError
  case class LectureNotFound(lectureId: String) extends ApiError
  case object LectureParsingError               extends ApiError
  case object UnknownError                      extends ApiError
  case object AlarmBatchError                   extends ApiError

  // Http model
  case class CreateUser(username: String, email: String)

  case class CreateAlarm(user: User, lectureId: String)

  case class UpdateUser(email: String)

}
