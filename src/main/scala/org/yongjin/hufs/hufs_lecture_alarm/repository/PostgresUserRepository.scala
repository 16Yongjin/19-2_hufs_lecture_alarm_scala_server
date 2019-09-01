package org.yongjin.hufs.hufs_lecture_alarm.repository

import cats.effect.Async
import cats.syntax.applicativeError._
import cats.data.OptionT
import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.UserRepository
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.invariant.UnexpectedEnd
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.util.transactor.Transactor

// It requires a created database `users` with db user `postgres` and password `postgres`. See `users.sql` file in resources.
class PostgresUserRepository[F[_]: Async](xa: Transactor[F]) extends UserRepository[F] {

  def addUserAlarm(createAlarm: CreateAlarm): OptionT[F, List[Lecture]] = {

    println(createAlarm)

    val addAlarm = UserStatement.addUserAlarm(createAlarm.user, createAlarm.lectureId).run
    val userLectures =
      UserStatement.findUserAlarms(createAlarm.user).stream.map(_.toLecture).compile.toList

    val program = for {
      -        <- addAlarm
      lectures <- userLectures
    } yield lectures

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def findUserAlarms(user: User): OptionT[F, List[Lecture]] = {
    val statement = UserStatement.findUserAlarms(user)
    val program   = statement.stream.map(_.toLecture).compile.toList

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

  def removeUserAlarm(user: User, lectureId: String): OptionT[F, List[Lecture]] = {
    val removeAlarm  = UserStatement.removeUserAlarm(user, lectureId).run
    val userLectures = UserStatement.findUserAlarms(user).stream.map(_.toLecture).compile.toList

    val lectures = for {
      _        <- removeAlarm
      lectures <- userLectures
    } yield lectures

    val res = lectures.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)

  }

  def listLectures(courseId: String): OptionT[F, List[Lecture]] = {
    val program = UserStatement
      .listLectures(courseId)
      .stream
      .map(_.toLecture)
      .compile
      .toList

    val res = program.map(Option.apply).transact(xa).recoverWith {
      case UnexpectedEnd => Async[F].delay(None)
    }

    OptionT(res)
  }

}

object UserStatement {

  def findUserAlarms(user: User): Query0[LectureDTO] = {
    sql"""SELECT L.id, L.course, L.index, L.name, L.professor, L.time
          FROM alarm AS A INNER JOIN lecture AS L ON A.lecture_id = L.id
          WHERE A.user_id = ${user.id}
    """.query[LectureDTO]
  }

  def addUserAlarm(user: User, lectureId: String): Update0 = {
    sql"""
      INSERT INTO alarm (user_id, lecture_id)
      SELECT ${user.id}, $lectureId
      WHERE NOT EXISTS (
        SELECT id FROM alarm WHERE user_id=${user.id} AND lecture_id=${lectureId}
      );
 
    """.update
  }

  def removeUserAlarm(user: User, lectureId: String): Update0 = {
    sql"""DELETE FROM alarm WHERE user_id = ${user.id} AND lecture_id = ${lectureId}""".update
  }

  def listLectures(courseId: String): Query0[LectureDTO] = {
    sql"""SELECT id, course, index, name, professor, time FROM lecture 
          WHERE course = $courseId
    """.query[LectureDTO]
  }

}
