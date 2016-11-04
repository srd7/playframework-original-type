package dao

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import slick.driver.JdbcProfile

import models.{Cart, Product}
import models.typesafe._
import models.typesafe.MappedColumnTypeImplicits._

// play-slick のサンプリでは
// dbConfigProvider を Inject しているが、
// 複数テーブルをまたぐ transaction を扱いたいので
// ここには Inject しない。
@Singleton
class CartDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
{
  import driver.api._

  // テーブル定義
  // 詳しいことは UserDAO.scala に記載しているので、ここでは省略。
  // テーブル設計がガバガバなのは仕様。
  private[this] class Carts(tag: Tag) extends Table[Cart](tag, "CART") {
    val id        = column[CartId]("ID", O.AutoInc, O.PrimaryKey)
    val userId    = column[UserId]("USER_ID")
    val productId = column[ProductId]("PRODUCT_ID")

    def * = (id, userId, productId) <> (Cart.tupled, Cart.unapply)
  }

  private[this] val Carts = TableQuery[Carts]

  /**
   * ユーザIDから、その人のカートを取得する。
   * 同じ商品でも複数個カートのレコードが挿入される（クソ仕様）ので、
   * GROUP BY していい感じにする。
   * その実は Plain SQL でのオリジナル型の使用法の説明が主目的である。
   */
  def getCarts(userId: UserId): DBIO[Seq[(Product, Int)]] = {
    // Plain SQL で使う SetParameter と GetResult を import
    import models.typesafe.SetParameterImplicits._
    import models.typesafe.GetResultImplicits._

    sql"""
      SELECT
        p.*,
        COUNT(1)
      FROM
        CART AS c
      INNER JOIN
        PRODUCT AS p
      ON
        c.PRODUCT_ID = p.ID
      WHERE
        c.USER_ID = ${userId}
      GROUP BY
        c.USER_ID, p.ID
      ORDER BY
        p.ID
    """.as[(Product, Int)]
  }

  /**
   * カートにレコードを1件追加する。
   *
   * そもそもカートが、1つの商品につき1レコードというクソ仕様なので
   * 非常にアレなことになってる。
   */
  def insert(cart: Cart): DBIO[Int] = {
    Carts += cart
  }

  /**
   * カートからレコードを全件削除する。
   *
   * ProductId は文字列にすると product1 のようになったりするが、
   * 特に気にせずクエリ組み立てができる。
   */
  def removeOneRecord(userId: UserId, productId: ProductId): DBIO[Int] = {
    Carts.filter(cart =>
      cart.userId === userId &&
      cart.productId === productId
    ).delete
  }
}
