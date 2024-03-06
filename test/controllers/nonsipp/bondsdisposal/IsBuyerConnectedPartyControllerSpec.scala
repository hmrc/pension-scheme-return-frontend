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

package controllers.nonsipp.bondsdisposal

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.bondsdisposal.IsBuyerConnectedPartyController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.bondsdisposal.BuyerNamePage
import pages.nonsipp.bondsdisposal.IsBuyerConnectedPartyPage
import views.html.YesNoPageView

class IsBuyerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)
  private lazy val onPageLoad = routes.IsBuyerConnectedPartyController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.IsBuyerConnectedPartyController.onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(BuyerNamePage(srn, index, disposalIndex), buyerName)

  "IsBuyerConnectedPartyController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, disposalIndex, buyerName, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, IsBuyerConnectedPartyPage(srn, index, disposalIndex), true, userAnswers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, disposalIndex, buyerName, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
