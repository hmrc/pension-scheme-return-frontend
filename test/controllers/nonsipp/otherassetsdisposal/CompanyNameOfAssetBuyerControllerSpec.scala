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

import pages.nonsipp.otherassetsdisposal.CompanyNameOfAssetBuyerPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.TextInputView
import forms.TextFormProvider
import models.NormalMode
import utils.IntUtils.given
import controllers.nonsipp.otherassetsdisposal.CompanyNameOfAssetBuyerController._

class CompanyNameOfAssetBuyerControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val assetIndex = 1
  private val disposalIndex = 1
  private lazy val onPageLoad =
    routes.CompanyNameOfAssetBuyerController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.CompanyNameOfAssetBuyerController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  private val companyNameOfAssetBuyer = "test"

  "CompanyNameOfAssetBuyerController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView]
        .apply(form(injected[TextFormProvider]), viewModel(srn, assetIndex, disposalIndex, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex),
        companyNameOfAssetBuyer
      ) { implicit app => implicit request =>
        injected[TextInputView]
          .apply(
            form(injected[TextFormProvider]).fill(companyNameOfAssetBuyer),
            viewModel(srn, assetIndex, disposalIndex, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> companyNameOfAssetBuyer))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> companyNameOfAssetBuyer))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
