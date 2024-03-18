/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import utils.WithName
import play.api.mvc.JavascriptLiteral
import play.api.libs.json._
import play.api.libs.functional.syntax._

object HowSharesDisposed {

  sealed trait HowSharesDisposed {
    val name: String
  }

  case object Sold extends WithName("Sold") with HowSharesDisposed
  case object Redeemed extends WithName("Redeemed") with HowSharesDisposed
  case object Transferred extends WithName("Transferred") with HowSharesDisposed
  case class Other(details: String) extends WithName("Other") with HowSharesDisposed

  case object Other extends WithName("Other")

  implicit val jsLiteral: JavascriptLiteral[HowSharesDisposed] = {
    case Sold => Sold.name
    case Redeemed => Redeemed.name
    case Transferred => Transferred.name
    case Other(_) => Other.name
  }

  implicit val writes: Writes[HowSharesDisposed] = {
    case Sold => Json.obj("key" -> Sold.name)
    case Redeemed => Json.obj("key" -> Redeemed.name)
    case Transferred => Json.obj("key" -> Transferred.name)
    case Other(description) => Json.obj("key" -> Other.name, "value" -> description)
  }

  implicit val reads: Reads[HowSharesDisposed] =
    (__ \ "key")
      .read[String]
      .and((__ \ "value").readNullable[String])
      .tupled
      .flatMap {
        case (Sold.name, None) => Reads.pure(Sold)
        case (Redeemed.name, None) => Reads.pure(Redeemed)
        case (Transferred.name, None) => Reads.pure(Transferred)
        case (Other.name, value) => Reads.pure(Other(value.getOrElse("Error: description of Other not retrievable")))
        case unknown => Reads.failed(s"Failed to read HowSharesDisposed with unknown pattern $unknown")
      }

  implicit val format: Format[HowSharesDisposed] = Format(reads, writes)
}
