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

import play.api.libs.json.{Format, Json}
import play.api.mvc.JavascriptLiteral
import utils.WithName
object HowDisposed {

  sealed trait HowDisposed

  case object Sold extends WithName("Sold") with HowDisposed
  case object Transferred extends WithName("Transferred") with HowDisposed
  case class Other(details: String) extends HowDisposed

  case object Other extends WithName("other")

  implicit val jsLiteral: JavascriptLiteral[HowDisposed] = {
    case Sold => Sold.name
    case Transferred => Transferred.name
    case Other(_) => Other.name
  }

  private implicit val soldFormat: Format[HowDisposed.Sold.type] = Json.format[HowDisposed.Sold.type]
  private implicit val transferredFormat: Format[HowDisposed.Transferred.type] =
    Json.format[HowDisposed.Transferred.type]
  private implicit val otherFormat: Format[HowDisposed.Other] = Json.format[HowDisposed.Other]
  implicit val format: Format[HowDisposed] = Json.format[HowDisposed]
}
