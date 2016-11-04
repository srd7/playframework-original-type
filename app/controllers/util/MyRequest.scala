package controllers.util

import play.api.mvc._

import models.typesafe.UserId

/**
 * オリジナルリクエスト型のベース。
 * InsideRequest の場合は、リクエストにセッション情報を埋め込む。
 */
abstract class MyRequest[A](request: Request[A]) extends WrappedRequest(request)

/**
 * アクセスを絞るために、コンストラクタを private にする。
 * ただし MyRequest の一員なので、そこからは呼べるように、
 * private の scope を controllers.util までは許容する。
 */
class OutsideRequest[A] private[util] (request: Request[A]) extends MyRequest(request)
class LoginRequest[A] private[util] (request: Request[A]) extends MyRequest(request) {
  def session(userId: UserId) = request.session + (MySession.USER_ID -> userId.toString)
}
class InsideRequest[A] private[util] (
  request   : Request[A],
  // 外部から userId を呼び出せるように val にする。
  val userId: UserId
) extends MyRequest(request)

object MyRequest {
  def outside[A](request: Request[A]) = new OutsideRequest[A](request)
  def login[A](request: Request[A]) = new LoginRequest[A](request)
  def inside[A](request: Request[A], userId: UserId) = new InsideRequest[A](request, userId)
}
