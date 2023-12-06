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

  private implicit val soldFormat: Format[PensionSchemeType.RegisteredPS] =
    Json.format[PensionSchemeType.RegisteredPS]
  private implicit val transferredFormat: Format[PensionSchemeType.QualifyingRecognisedOverseasPS] =
    Json.format[PensionSchemeType.QualifyingRecognisedOverseasPS]
  private implicit val otherFormat: Format[PensionSchemeType.Other] = Json.format[PensionSchemeType.Other]

  implicit val format: Format[PensionSchemeType] = Json.format[PensionSchemeType]
}
