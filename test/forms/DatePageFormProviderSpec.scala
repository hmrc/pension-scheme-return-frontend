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

import config.Constants
import forms.behaviours.FieldBehaviours
import forms.mappings.errors.DateFormErrors
import play.api.data.{Form, FormError}

import java.time.LocalDate

class DatePageFormProviderSpec extends FieldBehaviours {

  private val formProvider = new DatePageFormProvider()

  val form: Form[LocalDate] = formProvider(
    DateFormErrors(
      "error.required.all",
      "error.required.day",
      "error.required.month",
      "error.required.year",
      "error.required.two",
      "error.invalid.date",
      "error.invalid.characters",
      List(
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            "error.after"
          )
      )
    )
  )

  "value" - {
    behave.like(fieldThatBindsValidDate(form, "testField"))
    behave.like(
      fieldThatBindsTooEarlyDate(
        form,
        "value",
        formError = FormError("value", "error.after")
      )
    )
    behave.like(mandatoryField(form, "value", "error.required.all"))
  }
}
