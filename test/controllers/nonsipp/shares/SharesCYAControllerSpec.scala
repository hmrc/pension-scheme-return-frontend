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

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.shares.SharesCYAController._
import eu.timepit.refined.refineMV
import models.ConditionalYesNo._
import models.SchemeHoldShare.Contribution
import models.TypeOfShares.ConnectedParty
import models.{CheckOrChange, ConditionalYesNo, Crn, IdentitySubject, IdentityType}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.shares._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{PsrSubmissionService, SchemeDateService}
import views.html.CheckYourAnswersView

class SharesCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockSchemeDateService, mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)
  private val taxYear = Some(Left(dateRange))
  private val subject = IdentitySubject.SharesSeller

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.SharesCYAController.onPageLoad(srn, index, checkOrChange)
  private def onSubmit(checkOrChange: CheckOrChange) = routes.SharesCYAController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), ConnectedParty)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Contribution)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, index), companyName)
    .unsafeSet(HowManySharesPage(srn, index), totalShares)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index), companyName)
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index), false)
    .unsafeSet(CostOfSharesPage(srn, index), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index), true)
    .unsafeSet(TotalAssetValuePage(srn, index), money)
    .unsafeSet(SharesTotalIncomePage(srn, index), money)

  "SharesCYAController" - {
    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                typeOfShare = ConnectedParty,
                holdShares = Contribution,
                whenDidSchemeAcquire = Some(localDate),
                companyNameRelatedShares = companyName,
                companySharesCrn = ConditionalYesNo.yes[String, Crn](crn),
                companyName,
                howManyShares = totalShares,
                identityType = Some(IdentityType.UKPartnership),
                recipientName = Some(companyName),
                recipientDetails = None,
                recipientReasonNoDetails = None,
                sharesFromConnectedParty = None,
                costOfShares = money,
                shareIndependentValue = true,
                totalAssetValue = None,
                sharesTotalIncome = money,
                checkOrChange
              )
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct ${checkOrChange.name} view")
      )
      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
      )
      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )
      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
