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

import play.api.libs.json.{Format, JsString, Json, Writes}
import play.api.mvc.JavascriptLiteral
import utils.WithName

sealed trait PensionSchemeType {
  val name: String

  def fold[A](aRegisteredPS: => A, aQualifyingRecognisedOverseasPS: => A, other: => A): A = this match {
    case PensionSchemeType.RegisteredPS(_) => aRegisteredPS
    case PensionSchemeType.QualifyingRecognisedOverseasPS(_) => aQualifyingRecognisedOverseasPS
    case PensionSchemeType.Other(_) => other

  }
}

object PensionSchemeType extends Enumerable.Implicits {

  case class RegisteredPS(code: String) extends WithName("registeredPS") with PensionSchemeType
  case object RegisteredPS extends WithName("registeredPS") with PensionSchemeType
  case class QualifyingRecognisedOverseasPS(code: String)
      extends WithName("qualifyingRecognisedOverseasPS")
      with PensionSchemeType
  case object QualifyingRecognisedOverseasPS extends WithName("qualifyingRecognisedOverseasPS") with PensionSchemeType
  case class Other(details: String) extends WithName("other") with PensionSchemeType
  case object Other extends WithName("other") with PensionSchemeType

  val values: List[PensionSchemeType] = List(RegisteredPS, QualifyingRecognisedOverseasPS, Other)

  implicit val enumerable: Enumerable[PensionSchemeType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[PensionSchemeType] = (value: PensionSchemeType) => value.name

  implicit val writes: Writes[PensionSchemeType] =
    Writes(value => JsString(value.toString))
}
