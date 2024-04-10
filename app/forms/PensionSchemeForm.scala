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

package forms

import forms.mappings.Mappings
import play.api.data.validation.Constraint
import play.api.data.Form

import javax.inject.Inject

class PensionSchemeForm @Inject() extends Mappings {

  def apply(
    requiredKey: String = "pensionScheme.error.required",
    wholeNumberKey: String = "pensionScheme.error.wholeNumber",
    nonNumericKey: String = "pensionScheme.error.nonNumeric",
    constraint: Constraint[Int] = inRange(0, Int.MaxValue, "pensionScheme.error.outOfRange")
  ): Form[Int] =
    Form(
      "value" -> int(
        requiredKey,
        wholeNumberKey,
        nonNumericKey
      ).verifying(constraint)
    )
}
