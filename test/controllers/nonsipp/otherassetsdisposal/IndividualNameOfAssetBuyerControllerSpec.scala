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

import pages.nonsipp.otherassetsdisposal.IndividualNameOfAssetBuyerPage
import views.html.TextInputView
import utils.IntUtils.given
import forms.TextFormProvider
import models.NormalMode
import controllers.nonsipp.otherassetsdisposal.IndividualNameOfAssetBuyerController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class IndividualNameOfAssetBuyerControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val assetIndex = 1
  private val disposalIndex = 1

  "IndividualNameOfAssetBuyerController" - {

    lazy val onPageLoad =
      routes.IndividualNameOfAssetBuyerController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
    lazy val onSubmit =
      routes.IndividualNameOfAssetBuyerController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView]
        .apply(form(injected[TextFormProvider]), viewModel(srn, assetIndex, disposalIndex, NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), recipientName) {
        implicit app => implicit request =>
          val preparedForm = form(injected[TextFormProvider]).fill(recipientName)
          injected[TextInputView].apply(preparedForm, viewModel(srn, assetIndex, disposalIndex, NormalMode))
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> recipientName))
    act.like(invalidForm(onSubmit, defaultUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
