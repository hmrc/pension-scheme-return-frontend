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

import utils.Transform
import play.api.libs.json.{Format, Json}

case class PensionCommencementLumpSum(lumpSumAmount: Money, designatedPensionAmount: Money) {
  val isZero: Boolean = lumpSumAmount.isZero && designatedPensionAmount.isZero
  val tuple: (Money, Money) = (lumpSumAmount, designatedPensionAmount)
}

object PensionCommencementLumpSum {
  implicit val formats: Format[PensionCommencementLumpSum] = Json.format[PensionCommencementLumpSum]

  implicit val transform: Transform[(Money, Money), PensionCommencementLumpSum] =
    new Transform[(Money, Money), PensionCommencementLumpSum] {

      override def to(a: (Money, Money)): PensionCommencementLumpSum = PensionCommencementLumpSum(a._1, a._2)

      override def from(b: PensionCommencementLumpSum): (Money, Money) = (b.lumpSumAmount, b.designatedPensionAmount)
    }
}
