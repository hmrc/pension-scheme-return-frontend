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

package views

import views.html.NameDOBView
import forms.NameDOBFormProvider
import viewmodels.models.NameDOBViewModel
import forms.mappings.errors.DateFormErrors

class NameDOBViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[NameDOBView]

    val form = injected[NameDOBFormProvider].apply(
      "memberDetails.firstName.error.required",
      "memberDetails.firstName.error.invalid",
      "memberDetails.firstName.error.length",
      "memberDetails.lastName.error.required",
      "memberDetails.lastName.error.invalid",
      "memberDetails.lastName.error.length",
      DateFormErrors(
        "memberDetails.dateOfBirth.error.required.all",
        "memberDetails.dateOfBirth.error.required.day",
        "memberDetails.dateOfBirth.error.required.month",
        "memberDetails.dateOfBirth.error.required.year",
        "memberDetails.dateOfBirth.error.required.two",
        "memberDetails.dateOfBirth.error.invalid.date",
        "memberDetails.dateOfBirth.error.invalid.characters"
      )
    )

    val viewModelGen = formPageViewModelGen[NameDOBViewModel]

    "NameDOBView" - {
      act.like(renderTitle(viewModelGen)(view(form, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(form, _), _.heading))
      act.like(renderInputWithLabel(viewModelGen)("firstName", view(form, _), _.page.firstName))
      act.like(renderInputWithLabel(viewModelGen)("lastName", view(form, _), _.page.lastName))
      act.like(renderForm(viewModelGen)(view(form, _), _.onSubmit))
      act.like(renderDateInput(viewModelGen)("dateOfBirth", view(form, _)))
      act.like(renderButtonText(viewModelGen)(view(form, _), _.buttonText))
    }
  }
}
