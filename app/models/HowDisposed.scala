/*
 * Copyright 2024 HM Revenue & Customs
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
object HowDisposed {

  sealed trait HowDisposed {
    val name: String
  }

  case object Sold extends WithName("Sold") with HowDisposed
  case object Transferred extends WithName("Transferred") with HowDisposed
  case class Other(details: String) extends WithName("Other") with HowDisposed

  case object Other extends WithName("Other")

  implicit val jsLiteral: JavascriptLiteral[HowDisposed] = {
    case Sold => Sold.name
    case Transferred => Transferred.name
    case Other(_) => Other.name
  }

  implicit val writes: Writes[HowDisposed] = {
    case Sold => Json.obj("key" -> Sold.name)
    case Transferred => Json.obj("key" -> Transferred.name)
    case Other(description) => Json.obj("key" -> Other.name, "value" -> description)
  }

  implicit val reads: Reads[HowDisposed] =
    (__ \ "key")
      .read[String]
      .and((__ \ "value").readNullable[String])
      .tupled
      .flatMap {
        case (Sold.name, None) => Reads.pure(Sold)
        case (Transferred.name, None) => Reads.pure(Transferred)
        case (Other.name, value) => Reads.pure(Other(value.getOrElse("Error: description of Other not retrievable")))
        case unknown => Reads.failed(s"Failed to read HowDisposed with unknown pattern $unknown")
      }

  implicit val format: Format[HowDisposed] = Format(reads, writes)
}
