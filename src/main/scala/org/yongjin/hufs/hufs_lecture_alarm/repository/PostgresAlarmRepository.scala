package org.yongjin.hufs.hufs_lecture_alarm.repository

import cats.instances.list._
import cats._
import cats.data._
import cats.effect.Async
import cats.syntax.applicativeError._
import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.AlarmRepository
import org.yongjin.hufs.hufs_lecture_alarm.repository._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.invariant.UnexpectedEnd
import doobie.util.query.Query0
import doobie.util.update.{Update0, Update}
import doobie.util.transactor.Transactor
import fs2.Stream

class PostgresAlarmRepository[F[_]: Async: Parallel](xa: Transactor[F]) extends AlarmRepository[F] {
  import cats.implicits._

  def findAlarmToAlert(lectureId: String): OptionT[F, Alarm] = {
    val program = AlarmStatement
      .findAlarmToAlert(lectureId)
      .unique
      .map(v => {
        println(s"findAlarmToAlert $v")
        v.toAlarm
      })
    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def findAllAlarmToAlert(lectureIds: List[String]): OptionT[F, List[Alarm]] = {
    println(s"findAllAlarmToAlert $lectureIds")
    lectureIds.parTraverse(findAlarmToAlert)
  }

  def findAlarmLectures(): OptionT[F, List[Lecture]] = {
    val statement = AlarmStatement.findAlarmLectures()
    val program   = statement.stream.map(_.toLecture).compile.toList

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def findAllUserToAlert(lectureId: String): OptionT[F, List[User]] = {
    val statement = AlarmStatement.findAllUserToAlert(lectureId)
    val program   = statement.stream.compile.toList

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def removeAlertedAlarm(lectureId: String): OptionT[F, Unit] = {
    val statement = AlarmStatement.removeAlertedAlarm(lectureId)
    val program   = statement.run.map(_ => ())

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def removeAlertedAlarms(lectureIds: List[String]): OptionT[F, List[Unit]] = {
    lectureIds.parTraverse(removeAlertedAlarm)
  }

  def addLectures(lectures: List[Lecture]): OptionT[F, String] = {
    val program =
      AlarmStatement.addLectures(lectures).map(_ => lectures.map(_.course).distinct.mkString(", "))

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

}

object AlarmStatement {
  import doobie.postgres.implicits._

  def findAlarmToAlert(
      lectureId: String
  ): Query0[AlarmDTO] = {
    sql"""SELECT array_agg(A.user_id) as users, L.id, L.course, L.index, L.name, L.professor, L.time
          FROM alarm AS A INNER JOIN lecture AS L ON A.lecture_id = L.id
          WHERE L.id = $lectureId
          GROUP BY L.id
    """.query[AlarmDTO]
  }

  def findAlarmLectures(): Query0[LectureDTO] = {
    sql"""SELECT DISTINCT L.id, L.course, L.index, L.name, L.professor, L.time
          FROM alarm AS A INNER JOIN lecture AS L ON A.lecture_id = L.id
    """.query[LectureDTO]
  }

  def findAllUserToAlert(lectureId: String): Query0[User] = {
    sql"SELECT user_id FROM alarm WHERE lecture_id = $lectureId".query[User]
  }

  def removeAlertedAlarm(lectureId: String): Update0 = {
    sql"DELETE FROM alarm WHERE lecture_id = $lectureId".update
  }

  def addLectures(lectures: List[Lecture]) = {
    val sql = """
      INSERT INTO lecture (id, course, index, name, professor, time) 
      VALUES (?, ?, ?, ?, ?, ?);
    """

    Update[Lecture](sql).updateMany(lectures)
  }

}
