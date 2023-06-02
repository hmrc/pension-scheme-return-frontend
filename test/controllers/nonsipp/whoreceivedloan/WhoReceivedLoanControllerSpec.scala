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

package controllers.nonsipp.whoreceivedloan

import WhoReceivedLoanController._
import controllers.ControllerBaseSpec
import forms.RadioListFormProvider
import models.ReceivedLoanType.{Individual, Other, UKCompany, UKPartnership}
import views.html.RadioListView

class WhoReceivedLoanControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhoReceivedLoanController.onPageLoad(srn)
  private lazy val onSubmit = routes.WhoReceivedLoanController.onSubmit(srn)

  "WhoReceivedLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn)
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
