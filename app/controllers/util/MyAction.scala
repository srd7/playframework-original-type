package controllers.util

import play.api.mvc._
import scala.concurrent.Future

import javax.inject.Inject

import exceptions.UserNotFoundError
import models.typesafe.UserId

/**
 * オリジナルアクション型のベース。
 * セッション情報を読み取る。
 * Action として使うには ActionBuilder を mixin する必要があるため、
 * self-type annotation で指定しておく。
 * self-type annotation については、
 * - http://www.ne.jp/asahi/hishidama/home/tech/scala/class.html#h_class.this
 * などを参照。
 * 簡単に言うと、
 * ActionBuilder を mixin する前提を与えることで、
 * ActionBuilder のリソースを使うことができるようになる。
 */
sealed trait MyAction { self: ActionBuilder[MyRequest] =>
  /** セッション情報を読み取り */
  protected[this] def getSession[A](request: Request[A]): Either[Throwable, MySession] = {
    // Option 型から Either 型に置き換える
    def optionToEither[A, B](maybeRight: Option[B], left: => A): Either[A, B] = {
      maybeRight.fold[Either[A, B]](Left(left))(Right(_))
    }

    // セッションから userId を探し、適切に変換し、 MySession に格納する
    // Scala 2.12 なら Either がモナドになっているので、
    // もうちょっとマシな書き方 ( .right いらない ) ができる。
    for {
      userIdStr <- optionToEither(request.session.get(MySession.USER_ID), new UserNotFoundError).right
      userId    <- UserId.fromString(userIdStr).right
    } yield MySession(userId)
  }
}

/** ログインしていない状態でのアクション。 */
class OutsideAction extends MyAction with ActionBuilder[OutsideRequest] {
  /**
   * アクション実行部
   *
   * ここがアクションの核。
   * セッションを読み出して条件分岐する。
   * ここは OutsideAction なので、ログインしていないべき。
   * ログインしている場合は /home に飛ばす。
   *
   * @param request: PlayFramework が準備してくれる、オリジナルのリクエスト。
   * @param block: Controller側で我々が定義される実行部。
   * @return リクエストの実行結果。
   *
   * すなわち、PlayFramework の標準機能で送られてくる Request に対し、
   * なんらかの共通処理（この場合はセッションでの条件分岐）を行う。
   * さらに、リクエストになんらかの付加情報を与える（今回は何も付加しないが）。
   * Controller で定義されているアクション本体を通すが、
   * それを分岐して通さないことも可能である。
   *
   */
  override def invokeBlock[A](request: Request[A], block: OutsideRequest[A] => Future[Result]): Future[Result] = {
    // [[MyAction]] で定義した getSession
    this.getSession(request).fold(
      // セッションがないとき => OK。本体実行
      _ => block(MyRequest.outside(request)),
      // セッションがあるとき => リダイレクト
      _ => Future.successful(Results.Redirect("/home"))
    )
  }
}

/**
 * ログイン実行のアクション。
 *
 * ログインしていなアクションと似ているが、
 * ログイン実行のために DB アクセス権が必要になる。
 */
class LoginAction extends MyAction with ActionBuilder[LoginRequest] {
  /**
   * アクション実行部
   *
   * 基本的に [[OutsideAction.invokeBlock]] と同じ。
   */
  override def invokeBlock[A](request: Request[A], block: LoginRequest[A] => Future[Result]): Future[Result] = {
    // [[MyAction]] で定義した getSession
    this.getSession(request).fold(
      // セッションがないとき => OK。本体実行
      _ => block(MyRequest.login(request)),
      // セッションがあるとき => リダイレクト
      _ => Future.successful(Results.Redirect("/home"))
    )
  }
}

/**
 * ログインしている人向けのアクション。
 *
 * セッションから UserId を取得し、さらにDBアクセス権がある。
 */
class InsideAction extends MyAction with ActionBuilder[InsideRequest] {
  /**
   * アクション実行部
   *
   * 基本的に [[OutsideAction.invokeBlock]] と同じ。
   * 異なる点として、今回はセッションがある場合に成功する。
   */
  override def invokeBlock[A](request: Request[A], block: InsideRequest[A] => Future[Result]): Future[Result] = {
    // [[MyAction]] で定義した getSession
    this.getSession(request).fold(
      // セッションがないとき => リダイレクト
      _ => Future.successful(Results.Redirect("/")),
      // セッションがあるとき => UserId を取り出してアクション実行
      session => block(MyRequest.inside(request, session.userId))
    )
  }
}

/**
 * [[MyAction]] の Companion Object。
 *
 * 普通に MyAction と呼び出したら InsideAction になる。
 * 変換関数を噛ませてやることで、 OutsideAction/LoginAction になる。
 *
 * InsideAction をデフォルトにしているのは、
 * アプリ作成においてログイン済みの項目が一番種類が多いからである。
 */
object MyAction extends InsideAction {
  // 変換関数
  // MyAction.outside とすると OutsideAction になる。
  def outside = new OutsideAction
  def login = new LoginAction
}
