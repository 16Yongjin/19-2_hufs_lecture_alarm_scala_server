package org.yongjin.hufs.hufs_lecture_alarm.service

import cats.data.{OptionT, EitherT}
import cats.effect.{Async}
import cats._
import cats.data._
import cats.implicits._

import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.AlarmRepository
import org.yongjin.hufs.hufs_lecture_alarm.scraper.HttpClient
import org.yongjin.hufs.hufs_lecture_alarm.scraper.CourseInfo

class AlarmService[F[_]: Async: Parallel](
    alarmRepo: AlarmRepository[F]
)(
    client: HttpClient[F]
) {

  def alarmBatch() = {
    val res = for {
      lectures <- alarmRepo.findAlarmLectures
      _        <- OptionT.pure[F](println("알람 배치"))
      _        <- checkCourseAndSendAlarms(lectures)
    } yield ()

    res.toRight[ApiError](AlarmBatchError)
  }

  // 강의 목록을 코스 별로 구분하고 각 그룹마다 빈 강의 알람 보냄
  def checkCourseAndSendAlarms(lectures: List[Lecture]): OptionT[F, List[List[Unit]]] = {
    lectures.toCourseIndexMap.parTraverse(checkCourseAndSendAlarm _)
  }

  // 한 코스에서 빈 강의를 찾아낸 다음 각 강의에 등록된 유저에게 알람을 보냄
  def checkCourseAndSendAlarm(courseMap: (String, List[Int])): OptionT[F, List[Unit]] = {
    println(courseMap)
    client
      .checkIndexByCourse(courseMap._1, courseMap._2)
      .flatMap(findAlarmAndSendAlarms)
  }

  // 강의들을 등록한 유저들 각각에게 알람 보냄
  def findAlarmAndSendAlarms(lectureIds: List[String]): OptionT[F, List[Unit]] = {
    lectureIds.parTraverse(findAlarmAndSendAlarm _)
  }

  // 한 강의를 등록한 유저들에게 알람을 보냄
  def findAlarmAndSendAlarm(lectureId: String): OptionT[F, Unit] = {
    val res = for {
      alarm <- alarmRepo.findAlarmToAlert(lectureId)
      _     <- OptionT.pure[F](println(s"찾은 알람 $alarm"))
      _     <- client.sendAlarm(alarm)
      _     <- alarmRepo.removeAlertedAlarm(alarm.lecture.id)

    } yield ()

    res.map(_ => println(s"$lectureId 강의 알람 보냄"))
  }

  def addLecturesBatch(start: Int): EitherT[F, ApiError, String] = {
    val courseIds = CourseInfo.courses.values.drop(start * 10).take(10).toList

    val addedCourseIds = for {
      lectures       <- client.lecturesList(courseIds)
      _              <- OptionT.pure[F](println(lectures.length))
      addedCourseIds <- alarmRepo.addLectures(lectures)
    } yield addedCourseIds

    addedCourseIds.toRight[ApiError](UnknownError)
  }

}
