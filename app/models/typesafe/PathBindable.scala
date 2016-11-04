package models.typesafe

import play.api.mvc.PathBindable

// Play のルーティングで使うマッパー
// これがあれば conf/routes でオリジナルIDを使うことができる。
object PathBindableImplicits {

  // try して Either を返す。
  private[this] def tryEither[T, U](t: T)(f: T => U): Either[Throwable, U] = {
    try { Right(f(t)) } catch { case e: Throwable => Left(e) }
  }

  // PathBindable のインスタンスを implicit に定義しておく。
  // これがあれば Play のルーティング機能において
  // 自動的に bind/unbind してくれる。
  implicit def userIdPathBindable = new PathBindable[UserId] {
    // 文字列から UserId を取り出す。
    override def bind(key: String, value: String): Either[String, UserId] = {
      tryEither(value)(_.toLong).right.map(UserId.apply(_)).left.map(_.getMessage)
    }
    // UserId から文字列を取り出す。
    override def unbind(key: String, userId: UserId): String = {
      userId.value.toString
    }
  }
  // 基本的には UserId のものと同じ。
  // ただし、ここでは気まぐれに
  // product1 みたいな表記にしてみる。
  // 要するに 1 <--> product1 を定義してやればよい。
  // 変換は ProductId の定義のとこにあるので、それを利用する。
  implicit def productIdPathBindable = new PathBindable[ProductId] {

    override def bind(key: String, value: String): Either[String, ProductId] = {
      ProductId.fromString(value).left.map(_.getMessage)
    }
    override def unbind(key: String, productId: ProductId): String = {
      productId.toString
    }
  }
}
