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

package views

import forms.NameDOBFormProvider
import forms.mappings.errors.DateFormErrors
import play.api.test.FakeRequest
import views.html.NameDOBView

class NameDOBViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[NameDOBView]

    implicit val request = FakeRequest()

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

    "NameDOBView" - {
      act.like(renderTitle(nameDOBViewModelGen)(view(form, _), _.title.key))
      act.like(renderHeading(nameDOBViewModelGen)(view(form, _), _.heading))
      act.like(renderInputWithLabel(nameDOBViewModelGen)("firstName", view(form, _), _.firstName))
      act.like(renderInputWithLabel(nameDOBViewModelGen)("lastName", view(form, _), _.lastName))
      act.like(renderForm(nameDOBViewModelGen)(view(form, _), _.onSubmit))
      act.like(renderDateInput(nameDOBViewModelGen)("dateOfBirth", view(form, _)))
      act.like(renderSaveAndContinueButton(nameDOBViewModelGen)(view(form, _)))
    }
  }
}
