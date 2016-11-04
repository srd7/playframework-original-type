package controllers.form

import play.api.data._
import play.api.data.Forms._

import models.typesafe._
import models.typesafe.MyForms._

object MyForm {
  val loginForm = Form[(UserId, String)](
    tuple(
      "userId"   -> userId,
      "password" -> nonEmptyText
    )
  )

  val productIdForm = Form[ProductId](
    single("productId" -> productId)
  )
}
