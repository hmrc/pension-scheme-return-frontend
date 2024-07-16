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

import controllers.nonsipp.otherassetsheld.OtherAssetsCYAController._
import services.{PsrSubmissionService, SaveService, SchemeDateService}
import pages.nonsipp.otherassetsheld._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import uk.gov.hmrc.domain.Nino
import models._
import pages.nonsipp.common.IdentityTypePage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class OtherAssetsCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockSaveService: SaveService = mock[SaveService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[SaveService].toInstance(mockSaveService)
  )

  override protected def beforeAll(): Unit = {
    reset(mockSchemeDateService)
    reset(mockPsrSubmissionService)
  }

  private val index = refineMV[OneTo5000](1)
  private val taxYear = Some(Left(dateRange))
  private val subject = IdentitySubject.OtherAssetSeller

  private def onPageLoad(mode: Mode) = routes.OtherAssetsCYAController.onPageLoad(srn, index, mode)
  private def onSubmit(mode: Mode) = routes.OtherAssetsCYAController.onSubmit(srn, index, mode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), true)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
    .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
    .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index), ConditionalYesNo.yes[String, Nino](nino))
    .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
    .unsafeSet(CostOfOtherAssetPage(srn, index), money)
    .unsafeSet(IndependentValuationPage(srn, index), true)
    .unsafeSet(IncomeFromAssetPage(srn, index), money)

  "OtherAssetsCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                description = otherAssetDescription,
                isTangibleMoveableProperty = true,
                whyHeld = SchemeHoldAsset.Acquisition,
                acquisitionOrContributionDate = Some(localDate),
                sellerIdentityType = Some(IdentityType.Individual),
                sellerName = Some(individualName),
                sellerDetails = Some(nino.toString),
                sellerReasonNoDetails = None,
                isSellerConnectedParty = Some(true),
                totalCost = money,
                isIndependentValuation = Some(true),
                totalIncome = money,
                mode = mode
              )
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct ${mode.toString} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before({
            when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
            MockPsrSubmissionService.submitPsrDetails()
          })
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            verify(mockSaveService, times(2)).save(any())(any(), any())
            reset(mockPsrSubmissionService)
            reset(mockSaveService)
          })
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }
}
