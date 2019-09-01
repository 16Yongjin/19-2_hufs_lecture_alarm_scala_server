// package org.yongjin.hufs.hufs_lecture_alarm.service

// import cats.effect.IO
// import org.yongjin.hufs.hufs_lecture_alarm.TestUsers.users
// import org.yongjin.hufs.hufs_lecture_alarm.model.UserName
// import org.yongjin.hufs.hufs_lecture_alarm.repository.algebra.UserRepository

// object TestUserService {

//   private val testUserRepo: UserRepository[IO] =
//     (username: UserName) => IO {
//       users.find(_.username.value == username.value)
//     }

//   val service: UserService[IO] = new UserService[IO](testUserRepo)

// }
