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

package controllers.nonsipp.otherassetsheld

import pages.nonsipp.otherassetsheld.{WhenDidSchemeAcquireAssetsPage, WhyDoesSchemeHoldAssetsPage}
import config.Refined.Max5000
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.IdentitySubject.OtherAssetSeller
import controllers.nonsipp.otherassetsheld.WhyDoesSchemeHoldAssetsController._
import controllers.ControllerBaseSpec
import models.{CheckMode, NormalMode}
import models.SchemeHoldAsset.{Acquisition, Contribution, Transfer}

class WhyDoesSchemeHoldAssetsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val schemeHoldAssets = schemeHoldAssetsGen.sample.value

  private lazy val onPageLoad =
    routes.WhyDoesSchemeHoldAssetsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.WhyDoesSchemeHoldAssetsController.onSubmit(srn, index, NormalMode)
  private lazy val onSubmitCheckMode =
    routes.WhyDoesSchemeHoldAssetsController.onSubmit(srn, index, CheckMode)

  "WhyDoesSchemeHoldAssetsController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, index, schemeName, NormalMode)
      )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        WhyDoesSchemeHoldAssetsPage(srn, index),
        schemeHoldAssets,
        defaultUserAnswers
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), schemeHoldAssets)
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(schemeHoldAssets),
            viewModel(srn, index, schemeName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "Acquisition data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Acquisition.name))
    }

    "Contribution data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Contribution.name))
    }

    "Transfer data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Transfer.name))
    }

    "Acquisition in check mode when changing from contribution" - {
      "redirects to whenDidSchemeAcquireAssetsPage when no date is previously saved" - {
        act.like(
          redirectToPage(
            onSubmitCheckMode,
            controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
              .onPageLoad(srn, index, CheckMode),
            defaultUserAnswers
              .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution),
            "value" -> Acquisition.name
          )
        )
      }
      "redirects to Identity Type page when date is previously saved " - {
        act.like(
          redirectToPage(
            onSubmitCheckMode,
            controllers.nonsipp.common.routes.IdentityTypeController
              .onPageLoad(srn, index, CheckMode, OtherAssetSeller),
            defaultUserAnswers
              .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
              .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate),
            "value" -> Acquisition.name
          )
        )
      }
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
