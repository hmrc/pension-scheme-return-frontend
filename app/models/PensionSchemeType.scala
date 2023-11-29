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

import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Format, JsResult, JsString, JsValue, Json, Reads, Writes}
import play.api.mvc.JavascriptLiteral
import utils.WithName

sealed trait PensionSchemeType {
  val name: String
  val description: String

  def fold[A](aRegisteredPS: => A, aQualifyingRecognisedOverseasPS: => A, other: => A): A = this match {
    case PensionSchemeType.RegisteredPS(_) => aRegisteredPS
    case PensionSchemeType.QualifyingRecognisedOverseasPS(_) => aQualifyingRecognisedOverseasPS
    case PensionSchemeType.Other(_) => other

  }
}

object PensionSchemeType extends Enumerable.Implicits {

  case class RegisteredPS(description: String) extends WithName("registeredPS") with PensionSchemeType

  case object RegisteredPS extends WithName("registeredPS")

  case class QualifyingRecognisedOverseasPS(description: String)
      extends WithName("qualifyingRecognisedOverseasPS")
      with PensionSchemeType

  case object QualifyingRecognisedOverseasPS extends WithName("qualifyingRecognisedOverseasPS")

  case class Other(description: String) extends WithName("other") with PensionSchemeType

  case object Other extends WithName("other")

  val values: List[String] = List(RegisteredPS.name, QualifyingRecognisedOverseasPS.name, Other.name)

  implicit val jsLiteral: JavascriptLiteral[PensionSchemeType] = (value: PensionSchemeType) => value.name

  implicit val format = new Format[PensionSchemeType] {

    override def reads(json: JsValue): JsResult[PensionSchemeType] =
      (
        (__ \ "name")
          .read[String]
          .and((__ \ "description").read[String])
        )((a, b) => (a, b))
        .flatMap {
          case (Other.name, description) => Reads.pure(Other(description))
          case (QualifyingRecognisedOverseasPS.name, description) =>
            Reads.pure(QualifyingRecognisedOverseasPS(description))
          case (RegisteredPS.name, description) => Reads.pure(RegisteredPS(description))
          case (unknown, _) => Reads.failed(s"Failed to read PensionSchemeType with name $unknown")
        }
        .reads(json)

    implicit def writes(o: PensionSchemeType): JsValue =
      Json.obj(
        "name" -> o.name,
        "description" -> o.description
      )
  }
}
