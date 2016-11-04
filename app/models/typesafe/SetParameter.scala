package models.typesafe

import slick.jdbc.SetParameter

// Slick の SetParameter を生成。
// これがあれば Plain SQL で 例えば
// {{{
//   WHERE USER_ID = ${userId}
// }}}
// のように使える。
// ただし、 Table の見えるスコープだと
// テーブル定義がうまくいかないようなので（原因は不明）、
// その点だけ気を付ける。
object SetParameterImplicits {
  // 実体は Long 型なので、
  // 標準で用意されている Long 型をセットするものを利用させてもらう。
  implicit val userIdMapper = SetParameter[UserId] { (v, pp) =>
    SetParameter.SetLong(v.value, pp)
  }
}
