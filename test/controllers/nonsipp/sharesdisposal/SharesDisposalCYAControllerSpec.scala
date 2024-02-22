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

import config.Refined.{Max50, Max5000, OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.sharesdisposal.SharesDisposalCYAController._
import eu.timepit.refined.refineMV
import models.HowSharesDisposed.{HowSharesDisposed, Sold}
import models.IdentityType.Individual
import models.SchemeHoldShare.Acquisition
import models.SchemeId.Srn
import models.TypeOfShares._
import models.{CheckMode, ConditionalYesNo, IdentityType, Mode, Money, NormalMode, SchemeHoldShare, TypeOfShares}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.shares.{
  CompanyNameRelatedSharesPage,
  TypeOfSharesHeldPage,
  WhenDidSchemeAcquireSharesPage,
  WhyDoesSchemeHoldSharesPage
}
import pages.nonsipp.sharesdisposal._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import uk.gov.hmrc.domain.Nino
import views.html.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class SharesDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.SharesDisposalCYAController.onPageLoad(srn, shareIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.SharesDisposalCYAController.onSubmit(srn, shareIndex, disposalIndex, mode)

  private val shareIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  private val sharesType = SponsoringEmployer
  private val acquisitionType = Acquisition
  private val acquisitionDate = Some(localDate)

  private val howSharesDisposed = Sold

  private val dateSharesSold = Some(localDate)
  private val numberSharesSold = Some(totalShares)
  private val considerationSharesSold = Some(money)
  private val buyerIdentity = Some(Individual)
  private val nameOfBuyer = Some(buyerName)
//  private val buyerDetails = Some(nino)
//  private val buyerDetails = None
  private val buyerReasonNoDetails = Some(noninoReason)
  private val isBuyerConnectedParty = Some(true)
  private val isIndependentValuation = Some(true)

  private val dateSharesRedeemed = Some(localDate)
  private val numberSharesRedeemed = Some(totalShares)
  private val considerationSharesRedeemed = Some(money)

  private val sharesStillHeld = totalShares - 1

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), sharesType)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), acquisitionType)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndex), localDate)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), howSharesDisposed)
    .unsafeSet(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), dateSharesSold.get)
    .unsafeSet(HowManySharesSoldPage(srn, shareIndex, disposalIndex), numberSharesSold.get)
    .unsafeSet(TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex), considerationSharesSold.get)
    .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), buyerIdentity.get)
    .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex), nameOfBuyer.get)
//    .unsafeSet(
//      IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex),
//      ConditionalYesNo.yes[String, Nino](buyerDetails.get)
//    )
    .unsafeSet(
      IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex),
      ConditionalYesNo.no[String, Nino](buyerReasonNoDetails.get)
    )
    .unsafeSet(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex), isBuyerConnectedParty.get)
    .unsafeSet(IndependentValuationPage(srn, shareIndex, disposalIndex), isIndependentValuation.get)
    .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex), dateSharesSold.get)
    .unsafeSet(HowManySharesRedeemedPage(srn, shareIndex, disposalIndex), numberSharesSold.get)
    .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex), considerationSharesSold.get)
    .unsafeSet(HowManySharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)

  "SharesDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn: Srn,
                shareIndex: Max5000,
                disposalIndex: Max50,
                sharesType: TypeOfShares,
                companyName: String,
                acquisitionType: SchemeHoldShare,
                acquisitionDate: Option[LocalDate],
                howSharesDisposed: HowSharesDisposed,
                dateSharesSold: Option[LocalDate],
                numberSharesSold: Option[Int],
                considerationSharesSold: Option[Money],
                buyerIdentity: Option[IdentityType],
                nameOfBuyer: Option[String],
//                buyerDetails.map(_.toString): Option[String],
                None: Option[String],
                buyerReasonNoDetails: Option[String],
                isBuyerConnectedParty: Option[Boolean],
                isIndependentValuation: Option[Boolean],
                dateSharesRedeemed: Option[LocalDate],
                numberSharesRedeemed: Option[Int],
                considerationSharesRedeemed: Option[Money],
                sharesStillHeld: Int,
                schemeName: String,
                mode: Mode
              )
            )
          )
        }.withName(s"render correct ${mode} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${mode} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${mode} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${mode} mode")
      )
    }
  }
}
