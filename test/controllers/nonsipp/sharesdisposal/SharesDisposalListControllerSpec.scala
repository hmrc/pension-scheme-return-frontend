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

import pages.nonsipp.shares._
import controllers.nonsipp.sharesdisposal.SharesDisposalListController.{SharesDisposalData, _}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ListRadiosView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.{HowManySharesSoldPage, SharesDisposalProgress}
import forms.RadioListFormProvider
import models.{NormalMode, SchemeHoldShare, TypeOfShares}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

class SharesDisposalListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val page = 1
  private val shareIndexOne = 1
  private val shareIndexTwo = 2
  private val shareIndexThree = 3

  private lazy val onPageLoad = routes.SharesDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.SharesDisposalListController.onSubmit(srn, page)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(SharesCompleted(srn, shareIndexOne), SectionCompleted)
      .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexOne), TypeOfShares.SponsoringEmployer)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexOne), companyName)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndexOne), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndexOne), localDate)
      .unsafeSet(SharesCompleted(srn, shareIndexTwo), SectionCompleted)
      .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexTwo), TypeOfShares.Unquoted)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexTwo), companyName)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndexTwo), SchemeHoldShare.Contribution)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndexTwo), localDate)
      .unsafeSet(SharesCompleted(srn, shareIndexThree), SectionCompleted)
      .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexThree), TypeOfShares.ConnectedParty)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexThree), companyName)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndexThree), SchemeHoldShare.Transfer)

  private val incompleteUserAnswers = userAnswers
    .unsafeSet(HowManySharesSoldPage(srn, shareIndexOne, index1of50), 1)
    .unsafeSet(
      SharesDisposalProgress(srn, shareIndexOne, index1of50),
      SectionJourneyStatus.InProgress(
        routes.WhenWereSharesSoldController.onPageLoad(srn, shareIndexOne, index1of50, NormalMode).url
      )
    )

  private val sharesDisposalData = List(
    SharesDisposalData(
      shareIndexOne,
      typeOfShares = TypeOfShares.SponsoringEmployer,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Acquisition,
      acquisitionDate = Some(localDate)
    ),
    SharesDisposalData(
      shareIndexTwo,
      typeOfShares = TypeOfShares.Unquoted,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Contribution,
      acquisitionDate = Some(localDate)
    ),
    SharesDisposalData(
      shareIndexThree,
      typeOfShares = TypeOfShares.ConnectedParty,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Transfer,
      acquisitionDate = None
    )
  )

  "SharesDisposalListControllerSpec" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, sharesDisposalData, userAnswers))
    })

    act.like(
      redirectToPage(
        call = onSubmit,
        page = routes.WhenWereSharesSoldController.onPageLoad(srn, shareIndexOne, index1of50, NormalMode),
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
