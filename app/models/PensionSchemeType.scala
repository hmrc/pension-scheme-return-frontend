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

object PensionSchemeType {

  sealed trait PensionSchemeType {
    val name: String
  }
  case class RegisteredPS(description: String) extends WithName("registeredPS") with PensionSchemeType

  case object RegisteredPS extends WithName("registeredPS")

  case class QualifyingRecognisedOverseasPS(description: String)
      extends WithName("qualifyingRecognisedOverseasPS")
      with PensionSchemeType

  case object QualifyingRecognisedOverseasPS extends WithName("qualifyingRecognisedOverseasPS")

  case class Other(description: String) extends WithName("other") with PensionSchemeType

  case object Other extends WithName("other")

  val values: List[String] = List(RegisteredPS.name, QualifyingRecognisedOverseasPS.name, Other.name)

  implicit val jsLiteral: JavascriptLiteral[PensionSchemeType] = {
    case RegisteredPS(_) => RegisteredPS.name
    case QualifyingRecognisedOverseasPS(_) => QualifyingRecognisedOverseasPS.name
    case Other(_) => Other.name
  }

  implicit val writes: Writes[PensionSchemeType] = {
    case RegisteredPS(description) =>
      Json.obj(
        "key" -> RegisteredPS.name,
        "value" -> description.filterNot(_.isWhitespace).toUpperCase
      )
    case QualifyingRecognisedOverseasPS(description) =>
      Json.obj(
        "key" -> QualifyingRecognisedOverseasPS.name,
        "value" -> description.filterNot(_.isWhitespace).toUpperCase
      )
    case Other(description) =>
      Json.obj(
        "key" -> Other.name,
        "value" -> description.replaceAll("\n", " ").replaceAll("\r", " ")
      )
  }

  implicit val reads: Reads[PensionSchemeType] =
    (__ \ "key")
      .read[String]
      .and((__ \ "value").read[String])
      .tupled
      .flatMap {
        case (RegisteredPS.name, value) => Reads.pure(RegisteredPS(value))
        case (QualifyingRecognisedOverseasPS.name, value) => Reads.pure(QualifyingRecognisedOverseasPS(value))
        case (Other.name, value) => Reads.pure(Other(value))
        case unknown => Reads.failed(s"Failed to read PensionSchemeType with unknown pattern $unknown")
      }

  implicit val format: Format[PensionSchemeType] = Format(reads, writes)
}
