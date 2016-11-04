package models.typesafe

import play.api.data.FormError
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.format.Formats.{longFormat, stringFormat}

/**
 * Formで使う Formatter。
 * ここでは、オリジナル型への変換方法を与える。
 */
object FormatterImplicits {
  // フォームでは Long 型として取り扱われる。
  // UserId <--> Long の変換を教えてやる。
  // Note: class UserId の value や object UserId の apply が
  //       private[typesafe] なので、ここでは読み込むことができる。
  implicit private[typesafe] def userIdFormat = new Formatter[UserId] {
    override val format = Some(("format.user.id", Nil))
    def bind(key: String, data: Map[String, String]) = {
      // PlayFramework 標準の long 変換機能を利用
      longFormat.bind(key, data)
        .right.map(UserId(_))
        .left.map(_ => Seq(FormError(key, "user.id.parse.error", Nil)))
    }
    def unbind(key: String, value: UserId) = longFormat.unbind(key, value.value)
  }
  implicit private[typesafe] def productIdFormat = new Formatter[ProductId] {
    override val format = Some(("format.product.id", Nil))
    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data)
        .right.flatMap(ProductId.fromString(_))
        .left.map(_ => Seq(FormError(key, "product.id.parse.error", Nil)))
    }
    def unbind(key: String, value: ProductId) = stringFormat.unbind(key, value.toString)
  }

}

/**
 * 実際に form で使えるデータ。
 * これがあれば Form で自作型へのマッピングができる。
 */
object MyForms {
  import FormatterImplicits._

  // FormatterImplicits で定義した変換を読み取り
  // 自動的に変換フォームにしてくれる。
  val userId = of[UserId]
  val productId = of[ProductId]
}
