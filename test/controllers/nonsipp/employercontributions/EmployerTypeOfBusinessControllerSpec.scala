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

package controllers.nonsipp.employercontributions

import config.Refined.{Max300, Max50}
import controllers.ControllerBaseSpec
import controllers.nonsipp.employercontributions.EmployerTypeOfBusinessController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.IdentityType.{Other, UKCompany, UKPartnership}
import models.NormalMode
import pages.nonsipp.employercontributions.EmployerNamePage
import views.html.RadioListView

class EmployerTypeOfBusinessControllerSpec extends ControllerBaseSpec {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val index = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.EmployerTypeOfBusinessController.onPageLoad(srn, memberIndex, index, NormalMode)
  private lazy val onSubmit =
    routes.EmployerTypeOfBusinessController.onSubmit(srn, memberIndex, index, NormalMode)

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
