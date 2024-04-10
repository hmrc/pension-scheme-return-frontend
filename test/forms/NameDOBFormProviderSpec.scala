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

import forms.behaviours.FieldBehaviours
import config.Constants
import org.scalacheck.Gen.alphaChar
import models.NameDOB
import play.api.data.{Form, FormError}
import forms.mappings.errors.DateFormErrors

class NameDOBFormProviderSpec extends FieldBehaviours {

  private val formProvider = new NameDOBFormProvider()

  import formProvider._

  val form: Form[NameDOB] = formProvider(
    "firstName.error.required",
    "firstName.error.invalid",
    "firstName.error.length",
    "lastName.error.required",
    "lastName.error.invalid",
    "lastName.error.length",
    DateFormErrors(
      "dateOfBirth.error.required.all",
      "dateOfBirth.error.required.day",
      "dateOfBirth.error.required.month",
      "dateOfBirth.error.required.year",
      "dateOfBirth.error.required.two",
      "dateOfBirth.error.invalid.date",
      "dateOfBirth.error.invalid.characters",
      List(
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            "dateOfBirth.error.after"
          )
      )
    )
  )

  ".firstName" - {
    behave.like(fieldThatBindsValidData(form, "firstName", stringsWithMaxLength(nameMaxLength)))
    behave.like(mandatoryField(form, "firstName", "firstName.error.required"))

    val lengthUpperLimit = 50
    val lengthFormError = FormError("firstName", "firstName.error.length", List(nameMaxLength))
    behave.like(fieldLengthError(form, "firstName", lengthFormError, nameMaxLength + 1, lengthUpperLimit, alphaChar))

    behave.like(
      invalidAlphaField(
        form,
        fieldName = "firstName",
        errorMessage = "firstName.error.invalid",
        args = List(nameRegex)
      )
    )
  }

  ".lastName" - {
    behave.like(fieldThatBindsValidData(form, "lastName", stringsWithMaxLength(nameMaxLength)))
    behave.like(mandatoryField(form, "lastName", "lastName.error.required"))

    val lengthUpperLimit = 50
    val lengthFormError = FormError("lastName", "lastName.error.length", List(nameMaxLength))
    behave.like(fieldLengthError(form, "lastName", lengthFormError, nameMaxLength + 1, lengthUpperLimit, alphaChar))

    behave.like(
      invalidAlphaField(
        form,
        fieldName = "lastName",
        errorMessage = "lastName.error.invalid",
        args = List(nameRegex)
      )
    )
  }

  ".dateOfBirth" - {
    behave.like(fieldThatBindsValidDate(form, "dateOfBirth"))
    behave.like(
      fieldThatBindsTooEarlyDate(
        form,
        "dateOfBirth",
        formError = FormError("dateOfBirth", "dateOfBirth.error.after")
      )
    )
    behave.like(mandatoryField(form, "dateOfBirth", "dateOfBirth.error.required.all"))
  }
}
