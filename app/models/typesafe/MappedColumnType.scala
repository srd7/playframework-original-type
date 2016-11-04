package models.typesafe

import slick.driver.H2Driver.api._

// Slick のテーブル定義で使うマッパー。
// これがあれば Table 定義でオリジナルIDを使うことができる。
object MappedColumnTypeImplicits {
  // DB には bigint = Long として格納されているので
  // UserId <--> Long の変換を教えてやる。
  // Note: class UserId の value や object UserId の apply が
  //       private[typesafe] なので、ここでは読み込むことができる。
  implicit val userIdMapper = MappedColumnType.base[UserId, Long](_.value, UserId.apply)
  implicit val productIdMapper = MappedColumnType.base[ProductId, Long](_.value, ProductId.apply)
  implicit val cartidMapper = MappedColumnType.base[CartId, Long](_.value, CartId.apply)
  // パスワードは varchar = String として格納されているので
  // Password <--> String の変換を教えてやる。
  implicit val passwordMapper = MappedColumnType.base[Password, String](_.hash, Password.apply)
}
