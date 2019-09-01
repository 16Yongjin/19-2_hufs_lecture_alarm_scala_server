package org.yongjin.hufs.hufs_lecture_alarm

import cats._
import cats.implicits._
import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}

import org.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.Client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Properties
import fs2._
import java.util.Calendar
import java.{util => ju}

import scraper.HttpClient

// The only place where the Effect is defined. You could change it for `TaskApp` and `monix.eval.Task` for example.
object Server extends IOApp {

  def currentHour = Calendar.getInstance(ju.Locale.KOREA).get(Calendar.HOUR_OF_DAY)

  def timeFilter = 9 to 16 contains currentHour

  def loop(f: IO[Unit]): IO[Unit] = {
    Stream
      .repeatEval(f)
      .zipLeft(Stream.awakeEvery[IO](2.second).filter(_ => timeFilter))
      .compile
      .drain
  }

  override def run(args: List[String]): IO[ExitCode] = {

    BlazeClientBuilder[IO](global).resource.use { resource =>
      val client = new HttpClient[IO](resource)
      val server = new HttpServer[IO](client)

      (loop(server.alarmService) -> server.server).parTupled
      // server.server
        .as(ExitCode.Success)
    }

  }

}

class HttpServer[F[_]: ConcurrentEffect: ContextShift: Timer: Parallel](client: HttpClient[F]) {

  private val ctx = new Module[F](client)

  val alarmService = ctx.alarmService.alarmBatch.getOrElse(())

  def server: F[Unit] =
    BlazeServerBuilder[F]
      .bindHttp(Properties.envOrElse("PORT", "8080").toInt, "0.0.0.0")
      .withHttpApp(ctx.httpApp)
      .serve
      .compile
      .drain

}
