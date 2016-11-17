package test.controllers

import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers.defaultAwaitTimeout
import play.filters.csrf.CSRFAddToken

import org.specs2.mutable._

import scala.concurrent.{Future, ExecutionContext}

import javax.inject.Inject

import controllers.MyController
import dao._
import exceptions._
import logics.MyLogic
import models.User
import models.typesafe.{UserId, Password}

/**
 * Controller のテストのために Logic を Mock する。
 * Logic では DAO とやり取りして DB とのやり取りを行っているが、
 * Mock した Logic においては、それらを一切無視する。
 *
 * もしもこれが Logic のテストであるならば、
 * Logic に Inject される DAO 等を Mock するが、
 * 今回は Logic 自体が Mock であるため、
 * DAO はそのまま流用する。
 */
class MockMyLogic @Inject() (
  protected override val dbConfigProvider: DatabaseConfigProvider,
  userDAO   : UserDAO,
  productDAO: ProductDAO,
  cartDAO   : CartDAO
) extends MyLogic(dbConfigProvider, userDAO, productDAO, cartDAO) {
  /**
   * 今回はログインチェックのみをテストするので、
   * ログインに関する処理だけを ovrrride する。
   */
  override def executeLogin(userId: UserId, password: String)(implicit ec: ExecutionContext): Future[User] = {
    // Workaround 感は否めないが、
    // UserId の toString が数字をそのまま String に変えるのを利用して
    // パターンマッチにかける。
    // id = 1 なら成功
    // id = 2 ならユーザーが見つからず認証失敗
    // id = 3 ならユーザーは見つかったがパスワードが違う
    // それ以外ならエラー
    // とする。
    // 詳しい内容は app/logics/MyLogic.scala を参照のこと。
    userId.toString match {
      case "1" => Future.successful(User(userId, "John", Password(password)))
      case "2" => Future.failed(new UserNotFoundError)
      case "3" => Future.failed(new PasswordWrongError)
      case _   => Future.failed(new RuntimeException(s"Unsupported number ${userId}"))
    }
  }
}

/**
 * MyController のテスト。
 * MyLogic を Mock した状態でのテストを行う。
 */
class MyControllerSpec extends Specification {
  /**
   * Google Guice によってアプリケーションのインスタンスを作る際に、
   * 好きなインスタンスを Inject することができる。
   *
   * 今回は MyLogic を MockMyLogic に置き換えたインスタンスを作成する。
   */
  val mockApp: Application = new GuiceApplicationBuilder()
    // MyLogic を MockMyLogic に置き換える
    .bindings(bind[MyLogic].to[MockMyLogic])
    .build

  // テストする MyController のインスタンス。
  // MyLogic が MockMyLogic に置き換えられている。
  val myController = mockApp.injector.instanceOf[MyController]

  // 直接 Action を呼び出すと CSRF Filter を通らないため、
  // そのまま実行すると views.html.helper.CSRF.getToken で落ちる。
  // それを防ぐために明示的に CSRF Token を付与してやる。
  // ref: https://www.playframework.com/documentation/2.5.x/ScalaCsrf#getting-the-current-token
  val addToken = mockApp.injector.instanceOf[CSRFAddToken]


  /** MyForm.loginForm に渡せる形式の JSON を載せた FakeRequest を返す。 */
  def request(userId: Long): FakeRequest[AnyContent] = {
    val body = Json.obj("userId" -> userId.toString, "password" -> "hidden")
    FakeRequest().withBody(AnyContentAsJson(body))
  }

  "MyController" should {
    // Result.withSession において、
    // 動いているアプリケーションが必要なるので定義しておく。
    "Login request" in new WithServer(mockApp) {

      // ログインのアクションを CSRF Token 付与でラップし、
      // さらに JSON を受け取れるようにする
      def login = addToken(myController.login)

      val user1Response = Helpers.await(login(request(1)))
      val user2Response = Helpers.await(login(request(2)))
      val user3Response = Helpers.await(login(request(3)))
      val user4Response = Helpers.await(login(request(4)))

      // 上から順に
      // 1. 成功（ホームにリダイレクト）
      // 2. ユーザー見つからない
      // 3. パスワード不一致
      // 4. エラー
      // が返るはずである。

      user1Response.header.status must beEqualTo(Status.SEE_OTHER )
      user2Response.header.status must beEqualTo(Status.BAD_REQUEST)
      user3Response.header.status must beEqualTo(Status.BAD_REQUEST)
      user4Response.header.status must beEqualTo(Status.INTERNAL_SERVER_ERROR)
    }
  }



}
