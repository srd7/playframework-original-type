package models.typesafe

import slick.jdbc.GetResult

// Slick の値取得で使うマッパー。
// これがあれば PlainSQL からオリジナル型を取得できる。
object GetResultImplicits {
  implicit val getResultProductId = GetResult(p => ProductId(p.<<[Long]))

  // 本来なら型安全目的のオリジナルID型ではないものは
  // ここに書くべきではないが、
  // 無駄にファイルを作るのが面倒だったので、
  // ここに作ってしまう。
  implicit val getResultProduct = GetResult(p => models.Product(
    p.<<[ProductId],
    p.<<[String],
    p.<<[Int]
  ))
}
