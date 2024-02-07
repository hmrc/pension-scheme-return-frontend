/*
 * Copyright 2023 HM Revenue & Customs
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

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.shares.SharesListController.SharesData
import controllers.nonsipp.sharesdisposal.SharesDisposalListController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{SchemeHoldShare, TypeOfShares}
import pages.nonsipp.shares.{
  CompanyNameRelatedSharesPage,
  SharesCompleted,
  TypeOfSharesHeldPage,
  WhenDidSchemeAcquireSharesPage,
  WhyDoesSchemeHoldSharesPage
}
import viewmodels.models.SectionCompleted
import views.html.ListRadiosView

class SharesDisposalListControllerSpec extends ControllerBaseSpec {

  private val page = 1
  private val shareIndexOne = refineMV[Max5000.Refined](1)
  private val shareIndexTwo = refineMV[Max5000.Refined](2)
  private val shareIndexThree = refineMV[Max5000.Refined](3)

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

  private val sharesData = List(
    SharesData(
      shareIndexOne,
      typeOfShares = TypeOfShares.SponsoringEmployer,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Acquisition,
      acquisitionDate = Some(localDate)
    ),
    SharesData(
      shareIndexTwo,
      typeOfShares = TypeOfShares.Unquoted,
      companyName = companyName,
      acquisitionType = SchemeHoldShare.Contribution,
      acquisitionDate = Some(localDate)
    ),
    SharesData(
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
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, sharesData, userAnswers))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
