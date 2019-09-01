package org.yongjin.hufs.hufs_lecture_alarm.scraper

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

import org.yongjin.hufs.hufs_lecture_alarm.model.LectureWithEmptyCheck

object ParserTest {

  val lectureInfoIndices = List(3, 0, 4, 12, 15, 16)

  def pick[A] = (indices: List[Int]) => (xs: List[A]) => indices.map(xs apply _)

  val pickLectureInfo = pick[String](lectureInfoIndices)

  val peopleRegex = "(\\d+) / (\\d+)".r

  def checkIsEmpty(people: String): Boolean = {
    people match {
      case peopleRegex(now, max) => now < max
      case _                     => false
    }
  }

  val hangulRegex = "[ㄱ-ㅎㅏ-ㅣ가-힣]".r

  def hasHangul(str: String): Boolean = {
    hangulRegex.findFirstIn(str) match {
      case Some(_) => true
      case _       => false
    }
  }

  def trimEngTitle(str: String): String = {
    val idx = str.indexOf(" ")
    if (!hasHangul(str) || idx <= -1) str else str.take(idx)
  }
  def trimEngInfo(str: String): String = {
    val idx = str.indexOf("(") - 1
    if (idx <= -1) str else str.take(idx).trim()
  }

  val intRegex = """(\d+)""".r

  def parseLecture(args: List[String]): Option[LectureWithEmptyCheck] = {
    args match {
      case List(courseId, lectureId, intRegex(index), name, professor, time, people) =>
        Some(
          LectureWithEmptyCheck(
            lectureId,
            courseId,
            index.toInt,
            trimEngTitle(name),
            trimEngInfo(professor),
            trimEngInfo(time),
            checkIsEmpty(people)
          )
        )
      case _ => None
    }
  }

  def parseLectures(body: String): List[LectureWithEmptyCheck] = {

    val browser = JsoupBrowser()

    val doc = browser.parseString(body)

    val courseId = doc >> element("select[name=\"ag_crs_strct_cd\"]") >> element("option[selected]") >> attr(
      "value"
    )

    val trs = doc >> element("#premier1") >> elementList("tr")

    val ths = trs.drop(1).map(_ >> texts("td"))

    val lectures =
      ths
        .map(_.toList)
        .map(pickLectureInfo)
        .map(courseId :: _)
        .map(parseLecture)
        .flatten[LectureWithEmptyCheck]

    lectures
  }

}
