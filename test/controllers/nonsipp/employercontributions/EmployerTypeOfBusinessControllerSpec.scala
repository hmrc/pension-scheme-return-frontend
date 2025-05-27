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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions.EmployerNamePage
import controllers.nonsipp.employercontributions.EmployerTypeOfBusinessController._
import models.IdentityType.{Other, UKCompany, UKPartnership}
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import config.RefinedTypes.{Max300, Max50}
import controllers.ControllerBaseSpec

class EmployerTypeOfBusinessControllerSpec extends ControllerBaseSpec {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val index = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.EmployerTypeOfBusinessController.onPageLoad(srn, memberIndex.value, index.value, NormalMode)
  private lazy val onSubmit =
    routes.EmployerTypeOfBusinessController.onSubmit(srn, memberIndex.value, index.value, NormalMode)

  private val userAnswers =
    defaultUserAnswers.unsafeSet(EmployerNamePage(srn, memberIndex, index), employerName)

  "whoPurchasedLandOrPropertyController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, memberIndex, index, employerName, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "aUKCompany data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> UKCompany.name))
    }

    "aUKPartnership data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> UKPartnership.name))
    }

    "other data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Other.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
