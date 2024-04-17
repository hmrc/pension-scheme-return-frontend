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

import pages.nonsipp.otherassetsdisposal.{HowWasAssetDisposedOfPage, OtherAssetsDisposalProgress}
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.ListView
import eu.timepit.refined.refineMV
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import forms.YesNoPageFormProvider
import models.NormalMode
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed.{Other, Sold, Transferred}

class ReportedOtherAssetsDisposalListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportedOtherAssetsDisposalListController.onSubmit(srn, page)
  private lazy val otherAssetDisposalPage = routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode)

  private val page = 1
  private val otherAssetIndexOne = refineMV[Max5000.Refined](1)
  private val otherAssetIndexTwo = refineMV[Max5000.Refined](2)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val howOtherAssetsDisposedOne = Sold
  private val howOtherAssetsDisposedTwo = Transferred
  private val howOtherAssetsDisposedThree = Other(otherDetails)

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)
  private val otherAssetsDisposalsWithIndexes =
    Map((otherAssetIndexOne, disposalIndexes), (otherAssetIndexTwo, disposalIndexes))

  private val completedUserAnswers = defaultUserAnswers
  // Other Assets #1
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexOne), nameOfAsset)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexOne, disposalIndexOne), howOtherAssetsDisposedOne)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexOne, disposalIndexTwo), howOtherAssetsDisposedTwo)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexOne), SectionCompleted)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexOne, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexOne, disposalIndexTwo), SectionJourneyStatus.Completed)
    // Other Assets #2
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexTwo), nameOfAsset)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexTwo, disposalIndexOne), howOtherAssetsDisposedTwo)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexTwo, disposalIndexTwo), howOtherAssetsDisposedThree)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexTwo), SectionCompleted)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexTwo, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexTwo, disposalIndexTwo), SectionJourneyStatus.Completed)

  "ReportedOtherAssetsDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          page,
          otherAssetsDisposalsWithIndexes,
          completedUserAnswers
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        otherAssetDisposalPage,
        defaultUserAnswers
      ).withName("Not Started Journey")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
