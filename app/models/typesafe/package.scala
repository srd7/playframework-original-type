package models.typesafe

// オリジナル型たち。
// private[typesafe] としているのは、
// 各種マッパー定義の時点では value にアクセスできる必要があるから。
// 不必要にスコープを広げていないのは、それに頼ったコードを書かせないため。
// もっと具体的に言うと、マジックナンバーを封じる。
class UserId private (private[typesafe] val value: Long) extends AnyVal {
  override def toString = value.toString
}
object UserId {
  private[typesafe] def apply(value: Long) = new UserId(value)
  // セッションから読み取り時。
  def fromString(str: String): Either[Throwable, UserId] = {
    try {
      Right(UserId(str.toLong))
    } catch {
      case e: Throwable => Left(e)
    }
  }
}

class ProductId private (private[typesafe] val value: Long) extends AnyVal {
  override def toString = "product" + value.toString
}
object ProductId {
  private[typesafe] def apply(value: Long) = new ProductId(value)
  def fromString(str: String): Either[Throwable, ProductId] = {
    try {
      Right(ProductId(str.substring("product".length).toLong))
    } catch {
      case e: Throwable => Left(e)
    }
  }
}

class CartId private (private[typesafe] val value: Long) extends AnyVal {
  override def toString = value.toString
}
object CartId {
  private[typesafe] def apply(value: Long) = new CartId(value)
  // Slick では PrimaryKey の項は insert する時に捨てられるので、
  // 適当な値を入れられるようにしておく。
  def dummy = CartId(-1L)
}

// id だけじゃなくパスワードも typesafe にする。
class Password private (val hash: String) extends AnyVal {
  override def toString = "******"
}
object Password {
  def apply(hash: String) = new Password(hash)
}

