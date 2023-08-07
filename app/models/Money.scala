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
import utils.Transform

import java.text.DecimalFormat

case class Money(value: Double, displayAs: String) {

  def formattedDisplayAs(format: String): String =
    new DecimalFormat(format).format(value)

}

object Money {

  def apply(value: Double): Money =
    Money(value, value.toString)

  implicit val formats: Format[Money] = Json.format[Money]
}

case class MoneyInPeriod(moneyAtStart: Money, moneyAtEnd: Money)

object MoneyInPeriod {

  implicit val formats: Format[MoneyInPeriod] = Json.format[MoneyInPeriod]

  implicit val transform: Transform[(Money, Money), MoneyInPeriod] = new Transform[(Money, Money), MoneyInPeriod] {

    override def to(a: (Money, Money)): MoneyInPeriod = MoneyInPeriod(a._1, a._2)

    override def from(b: MoneyInPeriod): (Money, Money) = (b.moneyAtStart, b.moneyAtEnd)
  }

}
