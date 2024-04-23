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

import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import config.Refined.Max5000
import controllers.ControllerBaseSpec
import views.html.ListRadiosView
import controllers.nonsipp.otherassetsdisposal.StartReportingAssetsDisposalController.{AssetData, _}
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import viewmodels.models.SectionCompleted

class StartReportingAssetsDisposalControllerSpec extends ControllerBaseSpec {

  private val page = 1
  private val assetIndexOne = refineMV[Max5000.Refined](1)
  private val assetIndexTwo = refineMV[Max5000.Refined](2)
  private val assetIndexThree = refineMV[Max5000.Refined](3)

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

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
