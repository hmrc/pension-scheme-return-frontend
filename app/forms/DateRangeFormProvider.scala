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

package forms

import com.google.inject.Inject
import forms.mappings.Mappings
import forms.mappings.errors.DateFormErrors
import models.DateRange
import play.api.data.Form
import play.api.data.Forms.mapping

class DateRangeFormProvider @Inject()() extends Mappings {

  def apply(
    startDateErrors: DateFormErrors,
    endDateErrors: DateFormErrors,
    invalidRangeError: String,
    allowedRange: Option[DateRange],
    startDateAllowedDateRangeError: Option[String],
    endDateAllowedDateRangeError: Option[String],
    duplicateRangeError: Option[String],
    duplicateRanges: List[DateRange]
  ): Form[DateRange] =
    Form(
      mapping(
        "dates" -> dateRange(
          startDateErrors,
          endDateErrors,
          invalidRangeError,
          allowedRange,
          startDateAllowedDateRangeError,
          endDateAllowedDateRangeError,
          duplicateRangeError,
          duplicateRanges
        )
      )(identity)(Some(_))
    )
}
