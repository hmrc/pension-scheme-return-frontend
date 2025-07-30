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

package controllers.nonsipp.shares

import controllers.nonsipp.shares.{SharesCheckAndUpdateController => SharesController}
import pages.nonsipp.shares._
import models.IdentityType.UKPartnership
import utils.nonsipp.summary.SharesCheckAnswersUtils
import models.SchemeHoldShare.Acquisition
import utils.IntUtils.given
import config.Constants.incomplete
import models.{NormalMode, _}
import pages.nonsipp.common.{IdentityTypePage, PartnershipRecipientUtrPage}
import models.IdentitySubject.SharesSeller
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.PrePopCheckYourAnswersView
import models.TypeOfShares.SponsoringEmployer

class SharesCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad = controllers.nonsipp.shares.routes.SharesCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = controllers.nonsipp.shares.routes.SharesCheckAndUpdateController.onSubmit(srn, index)

  private val prePopUserAnswers = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), SponsoringEmployer)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.yes(crn))
    .unsafeSet(ClassOfSharesPage(srn, index), classOfShares)
    .unsafeSet(HowManySharesPage(srn, index), totalShares)
    .unsafeSet(IdentityTypePage(srn, index, SharesSeller), UKPartnership)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index), companyName)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index, SharesSeller), ConditionalYesNo.yes(utr))
    .unsafeSet(CostOfSharesPage(srn, index), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index), true)
    .unsafeSet(TotalAssetValuePage(srn, index), money)

  private val completedUserAnswers = prePopUserAnswers
    .unsafeSet(SharesTotalIncomePage(srn, index), money)

  "SharesCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, prePopUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          SharesController.viewModel(
            srn,
            index.value,
            SharesCheckAnswersUtils
              .viewModel(
                srn = srn,
                index = index,
                schemeName = schemeName,
                typeOfShare = SponsoringEmployer,
                holdShares = Acquisition,
                whenDidSchemeAcquire = Some(localDate),
                companyNameRelatedShares = companyName,
                companySharesCrn = ConditionalYesNo.yes(crn),
                classOfShares = classOfShares,
                howManyShares = totalShares,
                identityType = Some(UKPartnership),
                recipientName = Some(companyName),
                recipientDetails = Some(utr.value),
                recipientReasonNoDetails = None,
                sharesFromConnectedParty = None,
                costOfShares = money,
                shareIndependentValue = true,
                totalAssetValue = Some(money),
                sharesTotalIncome = Left(incomplete),
                mode = NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.withName("render correct view when prePopulation data missing")
    )

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          SharesController.viewModel(
            srn,
            index.value,
            SharesCheckAnswersUtils
              .viewModel(
                srn = srn,
                index = index,
                schemeName = schemeName,
                typeOfShare = SponsoringEmployer,
                holdShares = Acquisition,
                whenDidSchemeAcquire = Some(localDate),
                companyNameRelatedShares = companyName,
                companySharesCrn = ConditionalYesNo.yes(crn),
                classOfShares = classOfShares,
                howManyShares = totalShares,
                identityType = Some(UKPartnership),
                recipientName = Some(companyName),
                recipientDetails = Some(utr.value),
                recipientReasonNoDetails = None,
                sharesFromConnectedParty = None,
                costOfShares = money,
                shareIndependentValue = true,
                totalAssetValue = Some(money),
                sharesTotalIncome = Right(money),
                mode = NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.withName("render correct view when data complete")
    )

    act.like(
      redirectToPage(
        onSubmit,
        controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
