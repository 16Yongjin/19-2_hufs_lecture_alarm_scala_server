package org.yongjin.hufs.hufs_lecture_alarm

import cats.effect.IO

object IOAssertion {
  def apply[A](ioa: IO[A]): A = ioa.unsafeRunSync()
}
