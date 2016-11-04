package dao

// テーブルの Product と名前が衝突しているので回避
import scala.{Product => SProduct}

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import slick.driver.JdbcProfile

import models.Product
import models.typesafe._
import models.typesafe.MappedColumnTypeImplicits._

// play-slick のサンプリでは
// dbConfigProvider を Inject しているが、
// 複数テーブルをまたぐ transaction を扱いたいので
// ここには Inject しない。
@Singleton
class ProductDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
{
  import driver.api._

  // テーブル定義
  // 詳しいことは UserDAO.scala に記載しているので、ここでは省略。
  private[this] class Products(tag: Tag) extends Table[Product](tag, "PRODUCT") {
    val id    = column[ProductId]("ID", O.AutoInc, O.PrimaryKey)
    val name  = column[String]("NAME")
    val price = column[Int]("PRICE")

    def * = (id, name, price) <> (Product.tupled, Product.unapply)
  }

  private[this] val Products = TableQuery[Products]

  def getList: DBIO[Seq[Product]] = {
    Products.result
  }

  def findById(productId: ProductId): DBIO[Option[Product]] = {
    Products.filter(_.id === productId).result.headOption
  }
}
