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

package controllers.nonsipp.otherassetsdisposal

import pages.nonsipp.otherassetsdisposal.PartnershipBuyerNamePage
import views.html.TextInputView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec
import models.NormalMode
import controllers.nonsipp.otherassetsdisposal.PartnershipBuyerNameController._

class PartnershipBuyerNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "PartnershipBuyerNameController" - {

    lazy val onPageLoad =
      routes.PartnershipBuyerNameController.onPageLoad(srn, index, disposalIndex, NormalMode)
    lazy val onSubmit =
      routes.PartnershipBuyerNameController.onSubmit(srn, index, disposalIndex, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, disposalIndex, NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, PartnershipBuyerNamePage(srn, index, disposalIndex), recipientName) {
        implicit app => implicit request =>
          val preparedForm = form(injected[TextFormProvider]).fill(recipientName)
          injected[TextInputView].apply(preparedForm, viewModel(srn, index, disposalIndex, NormalMode))
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> recipientName))
    act.like(invalidForm(onSubmit, defaultUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
