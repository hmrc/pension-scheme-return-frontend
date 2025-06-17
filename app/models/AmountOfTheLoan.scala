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

case class AmountOfTheLoan(loanAmount: Money, optCapRepaymentCY: Option[Money], optAmountOutstanding: Option[Money]) {
  val asTuple: (Money, Option[Money], Option[Money]) = (loanAmount, optCapRepaymentCY, optAmountOutstanding)
  val hasMissingAnswers: Boolean = optCapRepaymentCY.isEmpty || optAmountOutstanding.isEmpty
}

object AmountOfTheLoan {
  implicit val formats: Format[AmountOfTheLoan] = Json.format[AmountOfTheLoan]

  private val amountOfTheLoanReadsBuilder =
    (JsPath \ "loanAmount")
      .read[Money]
      .and((JsPath \ "capRepaymentCY").readNullable[Money])
      .and((JsPath \ "amountOutstanding").readNullable[Money])

  implicit val amountOfTheLoanReads: Reads[AmountOfTheLoan] =
    amountOfTheLoanReadsBuilder.apply { (loanAmount, optCapRepaymentCY, optAmountOutstanding) =>
      AmountOfTheLoan(
        loanAmount,
        optCapRepaymentCY,
        optAmountOutstanding
      )
    }

  implicit val amountOfTheLoanWrites: Writes[AmountOfTheLoan] = Json.writes[AmountOfTheLoan]

  implicit val transform: Transform[(Money, Money, Money), AmountOfTheLoan] =
    new Transform[(Money, Money, Money), AmountOfTheLoan] {

      override def to(a: (Money, Money, Money)): AmountOfTheLoan =
        AmountOfTheLoan(a._1, Some(a._2), Some(a._3))

      override def from(b: AmountOfTheLoan): (Money, Money, Money) =
        (b.loanAmount, b.optCapRepaymentCY.get, b.optAmountOutstanding.get)
    }
}
