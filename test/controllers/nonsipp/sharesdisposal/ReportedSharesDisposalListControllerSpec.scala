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

package controllers.nonsipp.sharesdisposal

import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompleted, TypeOfSharesHeldPage}
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.{HowWereSharesDisposedPage, SharesDisposalProgress}
import models.HowSharesDisposed._
import forms.YesNoPageFormProvider
import models.NormalMode
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import views.html.ListView
import models.TypeOfShares._

class ReportedSharesDisposalListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReportedSharesDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportedSharesDisposalListController.onSubmit(srn, page)
  private lazy val sharesDisposalPage = routes.SharesDisposalController.onPageLoad(srn, NormalMode)

  private val page = 1
  private val shareIndexOne = refineMV[Max5000.Refined](1)
  private val shareIndexTwo = refineMV[Max5000.Refined](2)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val sharesTypeOne = SponsoringEmployer
  private val sharesTypeTwo = Unquoted

  private val howSharesDisposedOne = Sold
  private val howSharesDisposedTwo = Redeemed
  private val howSharesDisposedThree = Transferred
  private val howSharesDisposedFour = Other(otherDetails)

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)
  private val sharesDisposalsWithIndexes =
    Map((shareIndexOne, disposalIndexes), (shareIndexTwo, disposalIndexes))

  private val completedUserAnswers = defaultUserAnswers
  // Shares #1
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexOne), sharesTypeOne)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexOne), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, disposalIndexOne), howSharesDisposedOne)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, disposalIndexTwo), howSharesDisposedTwo)
    .unsafeSet(SharesCompleted(srn, shareIndexOne), SectionCompleted)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, disposalIndexTwo), SectionJourneyStatus.Completed)
    // Shares #2
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexTwo), sharesTypeTwo)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexTwo), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, disposalIndexOne), howSharesDisposedThree)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, disposalIndexTwo), howSharesDisposedFour)
    .unsafeSet(SharesCompleted(srn, shareIndexTwo), SectionCompleted)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexTwo, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexTwo, disposalIndexTwo), SectionJourneyStatus.Completed)

  "ReportedSharesDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          page,
          sharesDisposalsWithIndexes,
          completedUserAnswers
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        sharesDisposalPage,
        defaultUserAnswers
      ).withName("Not Started Journey")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
