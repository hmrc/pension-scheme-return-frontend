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

import models.DateRange
import uk.gov.hmrc.time.TaxYear
import play.api.data.Form
import forms.mappings.errors.DateFormErrors
import forms.mappings.Mappings
import com.google.inject.Inject
import play.api.data.Forms.mapping
import config.RefinedTypes.Max3

class DateRangeFormProvider @Inject()() extends Mappings {

  def apply(
    startDateErrors: DateFormErrors,
    endDateErrors: DateFormErrors,
    invalidRangeError: String,
    allowedRange: DateRange,
    startDateAllowedDateRangeError: String,
    endDateAllowedDateRangeError: String,
    overlappedStartDateError: String,
    overlappedEndDateError: String,
    duplicateRanges: List[DateRange],
    previousDateRangeError: Option[String],
    index: Max3,
    taxYear: TaxYear,
    errorStartBefore: String,
    errorStartAfter: String,
    errorEndBefore: String,
    errorEndAfter: String
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
          overlappedStartDateError,
          overlappedEndDateError,
          duplicateRanges,
          previousDateRangeError,
          index,
          taxYear,
          errorStartBefore,
          errorStartAfter,
          errorEndBefore,
          errorEndAfter
        )
      )(identity)(Some(_))
    )
}
