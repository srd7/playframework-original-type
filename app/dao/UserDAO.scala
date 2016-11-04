package dao

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import slick.driver.JdbcProfile

import models.User
import models.typesafe._
import models.typesafe.MappedColumnTypeImplicits._

// play-slick のサンプリでは
// dbConfigProvider を Inject しているが、
// 複数テーブルをまたぐ transaction を扱いたいので
// ここには Inject しない。
@Singleton
class UserDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
{
  import driver.api._

  // テーブル定義
  private[this] class Users(tag: Tag) extends Table[User](tag, "USER") {
    // ここで UserId 型を取り扱えるのは、
    // MappedColumnTypeImplicits で変換を定義しているからである。
    val id       = column[UserId]("ID", O.AutoInc, O.PrimaryKey)
    val name     = column[String]("NAME")
    val password = column[Password]("PASSWORD")

    def * = (id, name, password) <> (User.tupled, User.unapply)
  }

  // 実際にDBアクセスを書くときに使うインスタンス。
  // 他のテーブルとの join の必要があるので、
  // dao からはアクセス可能にしておく
  private[this] val Users = TableQuery[Users]

  // UserId から User を取得する。
  // 戻り値型 (DBIO型)については過去記事を参照。
  // ref: http://qiita.com/srd7/items/a24ef1713417f325cc11
  def findById(userId: UserId): DBIO[Option[User]] = {
    // Users の id を Rep[UserId] 型、
    // 要するに UserId 型を直接取り扱えるようにしているので、
    // これで後はライブラリ(Slick)側がいい感じにしてくれる。
    Users.filter(_.id === userId).result.headOption
  }
}
