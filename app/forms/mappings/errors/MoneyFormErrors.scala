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

package forms.mappings.errors

case class MoneyFormErrors(
  requiredKey: String,
  nonNumericKey: String,
  max: (Double, String),
  min: (Double, String) = (0, "error.tooSmall")
)

object MoneyFormErrors {

  def default(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonMoney",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    min: (Double, String) = (0, "error.tooSmall")
  ): MoneyFormErrors =
    MoneyFormErrors(requiredKey, nonNumericKey, max, min)
}

case class MoneyFormErrorValue(
  requiredKey: String,
  nonNumericKey: String,
  max: (Double, String),
  min: (Double, String)
)

object MoneyFormErrorValue {

  def default(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonMoney",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    min: (Double, String) = (Double.MinValue, "error.tooSmall")
  ): MoneyFormErrorValue =
    MoneyFormErrorValue(requiredKey, nonNumericKey, max, min)
}
