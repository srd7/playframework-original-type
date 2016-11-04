package controllers.util

import models.typesafe.UserId

/** このアプリで使うセッション情報 */
class MySession private (val userId: UserId)
object MySession {
  // セッションキー
  final val USER_ID = "user_id"
  def apply(userId: UserId) = new MySession(userId)
}
