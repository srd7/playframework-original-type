package models

import models.typesafe._

case class User(id: UserId, name: String, password: Password)
case class Product(id: ProductId, name: String, price: Int)
case class Cart(id: CartId, userId: UserId, productId: ProductId)
