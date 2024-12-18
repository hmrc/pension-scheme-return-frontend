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

import utils.Transform
import play.api.libs.json._
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class InterestOnLoan(loanInterestAmount: Money, loanInterestRate: Percentage, optIntReceivedCY: Option[Money]) {
  val asTuple: (Money, Percentage, Option[Money]) = (loanInterestAmount, loanInterestRate, optIntReceivedCY)
  val hasMissingAnswer: Boolean = optIntReceivedCY.isEmpty
}

object InterestOnLoan {
  implicit val formats: Format[InterestOnLoan] = Json.format[InterestOnLoan]

  private val interestOnLoanReadsBuilder =
    (JsPath \ "loanInterestAmount")
      .read[Money]
      .and((JsPath \ "loanInterestRate").read[Percentage])
      .and((JsPath \ "intReceivedCY").readNullable[Money])

  implicit val interestOnLoanReads: Reads[InterestOnLoan] =
    interestOnLoanReadsBuilder.apply(
      (loanInterestAmount, loanInterestRate, optIntReceivedCY) => {
        InterestOnLoan(
          loanInterestAmount,
          loanInterestRate,
          optIntReceivedCY
        )
      }
    )

  implicit val interestOnLoanWrites: Writes[InterestOnLoan] = Json.writes[InterestOnLoan]

  implicit val transform: Transform[(Money, Percentage, Money), InterestOnLoan] =
    new Transform[(Money, Percentage, Money), InterestOnLoan] {

      override def to(a: (Money, Percentage, Money)): InterestOnLoan =
        InterestOnLoan(a._1, a._2, Some(a._3))

      override def from(b: InterestOnLoan): (Money, Percentage, Money) =
        (b.loanInterestAmount, b.loanInterestRate, b.optIntReceivedCY.get)
    }
}
