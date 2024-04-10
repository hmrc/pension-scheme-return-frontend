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
import play.api.data.Forms.mapping
import models.NameDOB
import play.api.data.Form
import forms.mappings.errors.DateFormErrors

import javax.inject.Inject

class NameDOBFormProvider @Inject()() extends Mappings {

  val nameMaxLength = 35
  val nameRegex = "^[a-zA-Z\\-' ]+$"

  val firstName = "firstName"
  val lastName = "lastName"
  val dateOfBirth = "dateOfBirth"

  def apply(
    firstNameRequired: String,
    firstNameInvalid: String,
    firstNameLength: String,
    lastNameRequired: String,
    lastNameInvalid: String,
    lastNameLength: String,
    dateFormErrors: DateFormErrors
  ): Form[NameDOB] =
    Form(
      mapping(
        firstName -> text(firstNameRequired).verifying(
          firstError(
            regexp(nameRegex, firstNameInvalid),
            maxLength(nameMaxLength, firstNameLength)
          )
        ),
        lastName -> text(lastNameRequired).verifying(
          firstError(
            regexp(nameRegex, lastNameInvalid),
            maxLength(nameMaxLength, lastNameLength)
          )
        ),
        dateOfBirth -> localDate(dateFormErrors)
      )(NameDOB.apply)(NameDOB.unapply)
    )
}
