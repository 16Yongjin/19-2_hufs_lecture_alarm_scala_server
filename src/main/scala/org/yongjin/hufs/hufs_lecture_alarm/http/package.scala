package org.yongjin.hufs.hufs_lecture_alarm

import org.yongjin.hufs.hufs_lecture_alarm.model._
import io.circe.{Encoder, Json}

package object http {

  implicit val usernameEncoder: Encoder[UserName] = Encoder.instance {
    x => Json.fromString(x.value)
  }

  implicit val emailEncoder: Encoder[Email] = Encoder.instance {
    x => Json.fromString(x.value)
  }

}
