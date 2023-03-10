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

package controllers

import controllers.MemberDetailsController._
import eu.timepit.refined.refineMV
import forms.NameDOBFormProvider
import models.{NameDOB, NormalMode}
import pages.MemberDetailsPage
import views.html.NameDOBView

import java.time.LocalDate

class MemberDetailsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = controllers.routes.MemberDetailsController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = controllers.routes.MemberDetailsController.onSubmit(srn, refineMV(1), NormalMode)

  private val validForm = List(
    "firstName" -> "testFirstName",
    "lastName" -> "testLastName",
    "dateOfBirth.day" -> "12",
    "dateOfBirth.month" -> "12",
    "dateOfBirth.year" -> "2020",
  )

  private val nameDOB = NameDOB(
    "testFirstName",
    "testLastName",
    LocalDate.of(2020, 12, 12)
  )

  "MemberDetailsController" should {

    behave like renderView(onPageLoad) { implicit app =>
      implicit request =>
        injected[NameDOBView].apply(form(injected[NameDOBFormProvider]), viewModel(srn, refineMV(1), NormalMode))
    }

    behave like renderPrePopView(onPageLoad, MemberDetailsPage(srn, refineMV(1)), nameDOB) { implicit app =>
      implicit request =>
        val preparedForm = form(injected[NameDOBFormProvider]).fill(nameDOB)
        injected[NameDOBView].apply(preparedForm, viewModel(srn, refineMV(1), NormalMode))
    }

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)
    
    behave like saveAndContinue(onSubmit, validForm: _*)
    behave like invalidForm(onSubmit)
    behave like journeyRecoveryPage("onSubmit", onSubmit)
  }
}
