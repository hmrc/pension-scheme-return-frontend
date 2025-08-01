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

import pages.nonsipp.otherassetsdisposal.{AnyPartAssetStillHeldPage, OtherAssetsDisposalProgress}
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ListRadiosView
import utils.IntUtils.given
import controllers.nonsipp.otherassetsdisposal.StartReportingAssetsDisposalController.{AssetData, _}
import forms.RadioListFormProvider
import models.NormalMode
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

class StartReportingAssetsDisposalControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val page = 1
  private val assetIndexOne = 1
  private val assetIndexTwo = 2
  private val assetIndexThree = 3

  private lazy val onPageLoad = routes.StartReportingAssetsDisposalController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.StartReportingAssetsDisposalController.onSubmit(srn, page)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(WhatIsOtherAssetPage(srn, assetIndexOne), nameOfAsset)
      .unsafeSet(OtherAssetsCompleted(srn, assetIndexOne), SectionCompleted)
      .unsafeSet(WhatIsOtherAssetPage(srn, assetIndexTwo), nameOfAsset)
      .unsafeSet(OtherAssetsCompleted(srn, assetIndexTwo), SectionCompleted)
      .unsafeSet(WhatIsOtherAssetPage(srn, assetIndexThree), nameOfAsset)
      .unsafeSet(OtherAssetsCompleted(srn, assetIndexThree), SectionCompleted)

  private val incompleteUserAnswers = userAnswers
    .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, index1of50), true)
    .unsafeSet(
      OtherAssetsDisposalProgress(srn, assetIndexOne, index1of50),
      SectionJourneyStatus.InProgress(
        routes.HowWasAssetDisposedOfController.onPageLoad(srn, assetIndexOne, index1of50, NormalMode).url
      )
    )

  private val assetsData = List(
    AssetData(
      assetIndexOne,
      nameOfAsset
    ),
    AssetData(
      assetIndexTwo,
      nameOfAsset
    ),
    AssetData(
      assetIndexThree,
      nameOfAsset
    )
  )

  "StartReportingDisposalControllerSpec" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, assetsData, userAnswers))
    })

    act.like(
      redirectToPage(
        call = onSubmit,
        page = routes.HowWasAssetDisposedOfController.onPageLoad(srn, assetIndexOne, index1of50, NormalMode),
        userAnswers = incompleteUserAnswers,
        previousUserAnswers = emptyUserAnswers,
        form = "value" -> "1"
      ).withName("Redirect to incomplete record")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
