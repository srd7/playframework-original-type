package logics

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import slick.driver.JdbcProfile

import dao._
import exceptions._
import models.{User, Product, Cart}
import models.typesafe.{UserId, ProductId, CartId, Password}

@Singleton
class MyLogic @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  userDAO                       : UserDAO,
  productDAO                    : ProductDAO,
  cartDAO                       : CartDAO
) extends HasDatabaseConfigProvider[JdbcProfile]
{
  import driver.api._

  /**
   * パスワードをチェック。
   * 今回は簡単のために MD5 にしているが、
   * 実際は bcrypt のようなライブラリを使うことになるだろう。
   *
   * bcrypt について詳しくは説明しないが、
   * これらの暗号化アルゴリズムは、
   * 同じ値をハッシュ化しても毎回違う値になる。
   *
   * そのため、ハッシュ値を比較するというよりは、
   * ライブラリが用意したチェック用の関数を用いることになることになる。
   * ここではそれをイメージして、
   * 送信された生パスワードとDBから取得したハッシュ値を比較する形式にしている。
   */
  private[this] def checkPassword(sent: String, password: Password): Boolean = {
    // MD5 に変換。
    // 説明用だから、長々しく書いてるだけだよ。
    val md5 = java.security.MessageDigest.getInstance("MD5")
      .digest(sent.getBytes)
      .map("%02x".format(_))
      .mkString
    // 実際は Password の中身を取り出して check 関数に取り出すだろうから
    // ここでも中身を取り出すことにした。
    md5 == password.hash
  }

  /**
   * Option モナドを DBIO モナドに変換。
   *
   * よく使うのだが、いちいち fold を書いていると長々しくて面倒。
   * 厳密に型指定もしてやらないといけないからね。
   *
   * なおパラメータの orElse の型は => U （名前渡し）なので、
   * None にならない限り呼び出されることはない。
   * そのため、None になって初めて、
   * エラークラスはインスタンス化されることを覚えておこう。
   * ref: http://www.ne.jp/asahi/hishidama/home/tech/scala/def.html#h_call_by_name
   */
  private[this] def op2dbio[T, U <: Throwable](op: Option[T], caseNone: => U): DBIO[T] = {
    // DBIO モナドは DB 操作の 成功/失敗 情報のモナドであるが、
    // DBIO.successful とすると無条件で成功、
    // DBIO.failed とすると無条件で失敗になる。
    // 便利なので覚えておこう。
    op.fold[DBIO[T]](DBIO.failed(caseNone))(DBIO.successful(_))
  }

  /**
   * Boolean を DBIO モナドに変換。
   *
   * 上の Option => DBIO みたいな感じ。
   * 毎度毎度書くと長くなるので。
   * 名前渡しなのは言わずもがな。
   */
  private[this] def bool2dbio[T, U <: Throwable](flag: Boolean, caseTrue: => T, caseFalse: => U): DBIO[T] = {
    if (flag) { DBIO.successful(caseTrue) } else { DBIO.failed(caseFalse) }
  }

  /**
   * ログイン実行。
   *
   * ログインに完全に成功した場合のみ、User を返す。
   * どこかで失敗した場合は、
   * その失敗に対応したエラーの情報を持つ。
   */
  def executeLogin(userId: UserId, password: String)(implicit ec: ExecutionContext): Future[User] = {
    // ここでの flatMap (for - yield) は DBIO モナドについてである。
    // 詳しくは過去記事を参照。
    // ref: http://qiita.com/srd7/items/a24ef1713417f325cc11
    val action = for {
      // まず ID からユーザを取得する。
      // 間違った ID なら見つからない (Option[User])
      maybeUser <- userDAO.findById(userId)
      // ログインしたいので、見つからなかったらエラーとする。
      // なお見つからなくてエラーになった場合、
      // その瞬間に for-yield を抜ける。
      user      <- op2dbio(maybeUser, new UserNotFoundError)
      // ユーザが見つかったとして、
      // 送られたパスワードでログインできるか？
      // ちなみにだが引数型が (String, Password) なので、
      // 順番を覚える必要がない（間違えたらコンパイラが教えてくれる）。
      canLogin  =  checkPassword(password, user.password)
      // ログイン成功なら user をラップする。
      // 失敗ならエラーにする。
      // これを DBIO モナド
      result    <- bool2dbio(canLogin, user, new PasswordWrongError)
    } yield result

    // DBアクセスを実行して返す。
    // transactionally の効果は文字通り。
    db.run(action.transactionally)
  }

  /**
   * 商品リストを取得
   */
  def getProductList: Future[Seq[Product]] = {
    db.run(productDAO.getList)
  }

  /**
   * ID で指定された商品を取得
   */
  def getProduct(productId: ProductId): Future[Option[Product]] = {
    db.run(productDAO.findById(productId))
  }

  /**
   * カートの中身を取得する。
   */
  def getCarts(userId: UserId): Future[Seq[(Product, Int)]] = {
    db.run(cartDAO.getCarts(userId))
  }

  /**
   * カートに1件追加
   */
  def addCart(userId: UserId, productId: ProductId): Future[Int] = {
    val cart = Cart(
      // Slick においては PrimaryKey は insert 時に捨てられるので
      // 適当なダミー値を出せるようにしてある。
      id        = CartId.dummy,
      userId    = userId,
      productId = productId
    )

    db.run(cartDAO.insert(cart))
  }

  /**
   * カートから1件削除
   */
  def removeFromCart(userId: UserId, productId: ProductId): Future[Int] = {
    db.run(cartDAO.removeOneRecord(userId, productId))
  }
}
