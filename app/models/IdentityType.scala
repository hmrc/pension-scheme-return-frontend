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

import play.api.mvc.JavascriptLiteral
import utils.WithName

sealed trait IdentityType {
  val name: String

  def fold[A](anIndividual: => A, aUKCompany: => A, aUKPartnership: => A, other: => A): A = this match {
    case IdentityType.Individual => anIndividual
    case IdentityType.UKCompany => aUKCompany
    case IdentityType.UKPartnership => aUKPartnership
    case IdentityType.Other => other

  }
}

object IdentityType extends Enumerable.Implicits {

  case object Individual extends WithName("individual") with IdentityType
  case object UKCompany extends WithName("ukCompany") with IdentityType
  case object UKPartnership extends WithName("ukPartnership") with IdentityType
  case object Other extends WithName("other") with IdentityType

  val values: List[IdentityType] = List(Individual, UKCompany, UKPartnership, Other)

  implicit val enumerable: Enumerable[IdentityType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[IdentityType] = (value: IdentityType) => value.name
}
