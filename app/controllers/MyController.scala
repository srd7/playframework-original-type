package controllers

import scala.concurrent.Future
import javax.inject._

import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.SlickException

import controllers.util._
import controllers.form.MyForm
import exceptions._
import models.typesafe._
import logics.MyLogic

@Singleton
class MyController @Inject() (
  val messagesApi: MessagesApi,
  myLogic        : MyLogic
) extends Controller with I18nSupport {
  /**
   * 文字通り、ログインしていない状態でのページ。
   *
   * 外部ページという意味で MyAction.outside とする。
   * request は OutsideRequest 型であることに注意。
   */
  def outside = MyAction.outside { implicit request: OutsideRequest[AnyContent] =>
    Ok(views.html.index(MyForm.loginForm))
  }

  /**
   * ログイン実行アクションを利用する。
   *
   * DBアクセスは Future 型になるため、
   * async をつける。
   */
  def login = MyAction.login.async { implicit request: LoginRequest[AnyContent] =>
    val boundForm = MyForm.loginForm.bindFromRequest

    boundForm.fold(
      formWithErrors => Future.successful(BadRequest(views.html.index(formWithErrors))),
      { case (userId, password) =>
        // ログイン実行。
        // 詳細は呼び出している関数を見てください。
        // ちなみに action は DBIO[User] 型。
        myLogic.executeLogin(userId, password).map { user =>
          // ログイン成功。
          // ホームにリダイレクトしてあげよう。
          Redirect(routes.MyController.home).withSession(request.session(user.id))
        } recover {
          // 失敗した場合の処理。
          // DBIO.failed を使った場合を含め、
          // エラーが発生した場合はこちらに呼ばれる。
          case e: UserNotFoundError => {
            // ユーザが見つからなかった場合。
            // => ユーザIDが間違っている。
            val formWithErrors = boundForm
              .withError("userId", "login.user.id.wrong")
            BadRequest(views.html.index(formWithErrors))
          }
          case e: PasswordWrongError => {
            // パスワードが合わなかった場合。
            // => ユーザIDは合っているが、パスワードが違う。
            // セキュリティの観点から、どちらが間違っているとは言わないことが普通だが、
            // 今は説明のためにパスワードが違うよと教えてあげる。
            val formWithErrors = boundForm
              .withError("password", "login.password.wrong")
            BadRequest(views.html.index(formWithErrors))
          }
          case e: SlickException => {
            Logger.error("login.slick.exception", e)
            // DBアクセスでエラーが発生した可能性がある。
            InternalServerError(views.html.error())
          }
          case e: Throwable => {
            Logger.error("login.other.exception", e)
            // その他のエラー。
            InternalServerError(views.html.error())
          }
        }
      }
    )
  }

  /**
   * ログアウト。
   * 内部ページなので MyAction をそのまま使う（と InsideAction になる）。
   */
  def logout = MyAction { implicit request: InsideRequest[AnyContent] =>
    Redirect(routes.MyController.outside).withNewSession
  }

  /**
   * ログイン時のトップページ。
   * 商品一覧とそのリンク、ログアウトボタンを表示する。
   */
  def home = MyAction.async { implicit request: InsideRequest[AnyContent] =>
    myLogic.getProductList.map { productList =>
      Ok(views.html.home(productList))
    } recover {
      case e: SlickException => {
        Logger.error("home.slick.exception", e)
        // DBアクセスでエラーが発生した可能性がある。
        InternalServerError(views.html.error())
      }
      case e: Throwable => {
        Logger.error("home.other.exception", e)
        // その他のエラー。
        InternalServerError(views.html.error())
      }
    }
  }
  /**
   * 商品を取得。
   * 見つからなかったら 404 を返す。
   */
  def showProduct(productId: ProductId) = MyAction.async { implicit request: InsideRequest[AnyContent] =>
    // getProduct は Option[Product] を返す。
    // そのためパターンマッチで有無をチェックする。
    myLogic.getProduct(productId).map {
      case Some(product) => Ok(views.html.product(product, MyForm.productIdForm))
      case None          => NotFound(views.html.notfound.product())
    } recover {
      case e: SlickException => {
        Logger.error("show.product.slick.exception", e)
        // DBアクセスでエラーが発生した可能性がある。
        InternalServerError(views.html.error())
      }
      case e: Throwable => {
        Logger.error("show.product.other.exception", e)
        // その他のエラー。
        InternalServerError(views.html.error())
      }
    }
  }

  /**
   * カートを取得。
   * カート内の商品と、その個数を表示する。
   */
  def showCart = MyAction.async { implicit request: InsideRequest[AnyContent] =>
    // セッションから取得した userId を用いる。
    val userId = request.userId

    myLogic.getCarts(userId).map { carts =>
      Ok(views.html.cart(carts))
    } recover {
      case e: SlickException => {
        Logger.error("show.cart.slick.exception", e)
        // DBアクセスでエラーが発生した可能性がある。
        InternalServerError(views.html.error())
      }
      case e: Throwable => {
        Logger.error("show.cart.other.exception", e)
        // その他のエラー。
        InternalServerError(views.html.error())
      }
    }
  }

  def addCart = MyAction.async { implicit request: InsideRequest[AnyContent] =>
    // セッションから取得した userId を用いる。
    val userId = request.userId

    // 説明で使いたいから Form を使ったけど、
    // これは普通URLに入れるよねっていう。
    MyForm.productIdForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.error())),
      productId => {
        myLogic.addCart(userId, productId).map { _ =>
          Ok(views.html.done())
        } recover {
          case e: SlickException => {
            Logger.error("add.cart.slick.exception", e)
            // DBアクセスでエラーが発生した可能性がある。
            InternalServerError(views.html.error())
          }
          case e: Throwable => {
            Logger.error("add.cart.other.exception", e)
            // その他のエラー。
            InternalServerError(views.html.error())
          }
        }
      }
    )

  }
  def removeCart(productId: ProductId) = MyAction.async { implicit request: InsideRequest[AnyContent] =>
    // セッションから差取得した userId を用いる。
    val userId = request.userId

    myLogic.removeFromCart(userId, productId).map { _ =>
      Ok(views.html.done())
    } recover {
      case e: SlickException => {
        Logger.error("remove.cart.slick.exception", e)
        // DBアクセスでエラーが発生した可能性がある。
        InternalServerError(views.html.error())
      }
      case e: Throwable => {
        Logger.error("remove.cart.other.exception", e)
        // その他のエラー。
        InternalServerError(views.html.error())
      }
    }
  }
}
