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

sealed trait ReceivedLoanType {
  val name: String

  def fold[A](anIndividual: => A, aUKCompany: => A, aUKPartnership: => A, other: => A): A = this match {
    case ReceivedLoanType.AnIndividual => anIndividual
    case ReceivedLoanType.AUKCompany => aUKCompany
    case ReceivedLoanType.AUKPartnership => aUKPartnership
    case ReceivedLoanType.Other => other

  }
}

object ReceivedLoanType extends Enumerable.Implicits {

  case object AnIndividual extends WithName("anIndividual") with ReceivedLoanType
  case object AUKCompany extends WithName("aUKCompany") with ReceivedLoanType
  case object AUKPartnership extends WithName("aUKPartnership") with ReceivedLoanType
  case object Other extends WithName("other") with ReceivedLoanType

  val values: List[ReceivedLoanType] = List(AnIndividual, AUKCompany, AUKPartnership, Other)

  implicit val enumerable: Enumerable[ReceivedLoanType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[ReceivedLoanType] = (value: ReceivedLoanType) => value.name
}
