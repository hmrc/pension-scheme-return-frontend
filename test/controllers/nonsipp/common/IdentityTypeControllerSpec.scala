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

package controllers.nonsipp.common

import config.Refined.OneTo9999999
import controllers.ControllerBaseSpec
import controllers.nonsipp.common.IdentityTypeController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.IdentityType.{Individual, Other, UKCompany, UKPartnership}
import models.{IdentitySubject, NormalMode}
import views.html.RadioListView

class IdentityTypeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  // TODO for all identity subjects

  val identitySubject: IdentitySubject = IdentitySubject.LoanRecipient
  private lazy val onPageLoad =
    controllers.nonsipp.common.routes.IdentityTypeController
      .onPageLoad(srn, index, NormalMode, identitySubject)
  private lazy val onSubmit =
    controllers.nonsipp.common.routes.IdentityTypeController
      .onSubmit(srn, index, NormalMode, identitySubject)

  "IdentityTypeController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider], identitySubject),
        viewModel(srn, index, NormalMode, identitySubject)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "anIndividual data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Individual.name))
    }

    "aUKCompany data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> UKCompany.name))
    }

    "aUKPartnership data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> UKPartnership.name))
    }

    "other data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Other.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
