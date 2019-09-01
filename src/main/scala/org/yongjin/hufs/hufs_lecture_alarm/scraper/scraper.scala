package org.yongjin.hufs.hufs_lecture_alarm.scraper

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.implicits._
import cats._
import cats.data._

import org.http4s._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.Uri.uri

import org.http4s.circe._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.yongjin.hufs.hufs_lecture_alarm.model._
import org.yongjin.hufs.hufs_lecture_alarm.LectureParser

import io.circe.syntax._
import io.circe.generic.auto._

class HttpClient[F[_]: ContextShift: Timer: Parallel](client: Client[F])(
    implicit F: ConcurrentEffect[F]
) extends Http4sClientDsl[F] {

  val parser = new LectureParser[F]()

  def request: F[Unit] = {

    val lectures = for {
      html     <- fetchCourseHtml("AKA_H1")
      lectures <- parser.parseLectures("AKA_H1")(html)
    } yield lectures

    lectures.flatMap(body => F.delay(println(body)))
  }

  def lectureList(courseId: String): OptionT[F, List[Lecture]] = {
    val lectures = for {
      html     <- fetchCourseHtml(courseId)
      lectures <- parser.parseLectures(courseId)(html)
    } yield lectures

    OptionT(lectures).flatTap(v => OptionT.pure[F](println(v.length)))
  }

  def lectureWithEmptyCheckList(courseId: String): OptionT[F, List[LectureWithEmptyCheck]] = {
    val lectures = for {
      html     <- fetchCourseHtml(courseId)
      lectures <- parser.parseLecturesWithEmptyCheck(courseId)(html)
    } yield lectures

    OptionT(lectures)
  }

  def lecturesList(courseIds: List[String]): OptionT[F, List[Lecture]] = {
    courseIds.parTraverse(lectureList _).map(_.foldLeft(List.empty[Lecture])(_ |+| _))
  }

  private def fetchCourseHtml(courseId: String): F[String] = {
    val form         = buildLectureForm(courseId)
    val url          = uri("https://wis.hufs.ac.kr/src08/jsp/lecture/LECTURE2020L.jsp")
    val req          = POST(form, url)
    val responseBody = client.expect[String](req)

    responseBody
  }

  private def buildLectureForm(courseId: String) = {
    UrlForm(
      "ag_ledg_year"    -> "2019",
      "ag_ledg_sessn"   -> "3",
      "ag_org_sect"     -> "A",
      "campus_sect"     -> courseId.takeRight(2),
      "gubun"           -> (if (courseId.startsWith("A")) "1" else "2"),
      "ag_crs_strct_cd" -> courseId,
      "ag_compt_fld_cd" -> courseId
    )
  }

  private def pick[A] = (indices: List[Int]) => (xs: List[A]) => indices.map(xs apply _)

  private def filterEmptyLectureId(
      indeces: List[Int]
  )(l: List[LectureWithEmptyCheck]): List[String] = {
    pick(indeces)(l).filter(_.isEmpty).map(_.id)
  }

  def checkIdx(courseId: String, indeces: List[Int]): OptionT[F, List[String]] = {
    lectureWithEmptyCheckList(courseId)
      .map(filterEmptyLectureId(indeces))
  }

  def checkIndexByCourse(courseId: String, indeces: List[Int]): OptionT[F, List[String]] = {
    lectureWithEmptyCheckList(courseId).map(filterEmptyLectureId(indeces))
  }

  def sendAlarm(alarm: Alarm): OptionT[F, String] = {

    val authorization =
      "key=AAAAF0fPUwk:APA91bGP8nfVgRPNlDMAlWp49b2OyJch2sVfIWYGdbTQ0QFDVmoOpzLuXRvAT9DuhMGxFcmgmcu2qQQEUUNSCpwWWV8GV_AsDTD8iABjWSz1kZ1mJRsS9iwODl7TR3j1ddIejTaQ8D_v"

    val postRequest = POST(
      AlarmMessage(alarm).asJson,
      Uri.uri("https://fcm.googleapis.com/fcm/send"),
      Header("Authorization", authorization)
    )

    OptionT(client.expect[String](postRequest).map(Option.apply))
  }

  def sendAlarms(alarms: List[Alarm]): OptionT[F, List[String]] = {
    println(alarms)

    alarms.parTraverse(sendAlarm)
  }

}
