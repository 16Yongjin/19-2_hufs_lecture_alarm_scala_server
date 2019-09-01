package org.yongjin.hufs.hufs_lecture_alarm

import cats.effect.{ConcurrentEffect}
import cats.implicits._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.yongjin.hufs.hufs_lecture_alarm.model._

class LectureParser[F[_]](
    implicit F: ConcurrentEffect[F]
) {

  private val lectureInfoIndices = List(3, 0, 4, 11, 14, 15)

  private def pick[A] = (indices: List[Int]) => (xs: List[A]) => indices.map(xs apply _)

  private val pickLectureInfo = pick[String](lectureInfoIndices)

  private val peopleRegex = "(\\d+) / (\\d+)".r

  private def checkIsEmpty(people: String): Boolean = {
    people.trim() match {
      case peopleRegex(now, max) => now.toInt < max.toInt
      case _                     => false
    }
  }

  private val hufsRegex = "HUFS ".r

  private def startsWithhufs(str: String): Boolean = {
    hufsRegex.findFirstIn(str) match {
      case Some(_) => true
      case _       => false
    }
  }

  private val hangulRegex = "[ㄱ-ㅎㅏ-ㅣ가-힣]".r

  private def hasHangul(str: String): Boolean = {
    hangulRegex.findFirstIn(str) match {
      case Some(_) => true
      case _       => false
    }
  }

  private def trimEngTitle(str: String): String = {
    val idx = str.indexOf(" ")

    if (startsWithhufs(str) && str.indexOf(" (") > 0) str.take(str.indexOf(" ("))
    else if (!hasHangul(str) || idx <= -1) str
    else str.take(idx)
  }

  private def trimEngInfo(str: String): String = {
    val idx = str.indexOf("(") - 1
    if (idx <= -1) str else str.take(idx).trim()
  }

  private val intRegex = """(\d+)""".r

  private def parseLectureWithEmptyCheck(args: List[String]): Option[LectureWithEmptyCheck] = {
    args match {
      case List(courseId, lectureId, intRegex(index), name, professor, time, people) =>
        Some(
          LectureWithEmptyCheck(
            lectureId,
            courseId,
            index.toInt - 1,
            trimEngTitle(name),
            trimEngInfo(professor),
            trimEngInfo(time),
            checkIsEmpty(people)
          )
        )
      case _ => None
    }
  }

  def parseLecturesWithEmptyCheck(
      courseId: String
  )(body: String): F[Option[List[LectureWithEmptyCheck]]] = {

    val browser = JsoupBrowser()

    val doc = browser.parseString(body)

    val trs = doc >> element("#premier1") >> elementList("tr")

    val ths = trs.drop(1).map(_ >> texts("td"))

    val lectures =
      ths
        .map(_.toList)
        .map(pickLectureInfo)
        .map(courseId :: _)
        .map(parseLectureWithEmptyCheck)
        .sequence

    F.delay(lectures)
  }

  def parseLectures(
      courseId: String
  )(body: String): F[Option[List[Lecture]]] = {
    parseLecturesWithEmptyCheck(courseId)(body).map(_.map(_.map(_.toLecture)))
  }
}
