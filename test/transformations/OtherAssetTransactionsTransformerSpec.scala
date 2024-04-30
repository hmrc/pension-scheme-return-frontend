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

package transformations

import play.api.test.FakeRequest
import pages.nonsipp.otherassetsdisposal._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import models.IdentityType.{Individual, UKCompany}
import controllers.TestValues
import models.requests.psr._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import play.api.mvc.AnyContentAsEmpty
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import pages.nonsipp.otherassetsheld._
import models.HowDisposed.{Other, Sold, Transferred}
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino
import models._
import models.SchemeHoldAsset.{Acquisition, Contribution, Transfer}
import com.softwaremill.diffx.generic.auto.diffForCaseClass
import models.requests.{AllowedAccessRequest, DataRequest}

class OtherAssetTransactionsTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with DiffShouldMatcher {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new OtherAssetTransactionsTransformer

  "OtherAssetTransactionsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn = srn, otherAssetDisposed = false)
      result shouldMatchTo List.empty
    }

    "should return transformed List without disposed other assets" in {
      val userAnswers = emptyUserAnswers
      //index-1
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(1)), Acquisition)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(1)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(1)), true)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(1)), money)
        .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, refineMV(1)), localDate)
        .unsafeSet(IndependentValuationPage(srn, refineMV(1)), false)
        .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.OtherAssetSeller), Individual)
        .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, refineMV(1)), true)
        .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, refineMV(1)), "individualSellerName")
        .unsafeSet(
          OtherAssetIndividualSellerNINumberPage(srn, refineMV(1)),
          ConditionalYesNo.no[String, Nino]("reason")
        )
        //index-2
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(2)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(2)), Contribution)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(2)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(2)), false)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(2)), money)
        .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, refineMV(2)), localDate)
        .unsafeSet(IndependentValuationPage(srn, refineMV(2)), true)
        //index-3
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(3)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(3)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(3)), Transfer)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(3)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(3)), false)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(3)), money)

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn, otherAssetDisposed = false)(request)
      result shouldMatchTo List(
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Acquisition,
          optDateOfAcqOrContrib = Some(localDate),
          costOfAsset = money.value,
          optPropertyAcquiredFromName = Some("individualSellerName"),
          optPropertyAcquiredFrom = Some(
            PropertyAcquiredFrom(
              identityType = Individual,
              idNumber = None,
              reasonNoIdNumber = Some("reason"),
              otherDescription = None
            )
          ),
          optConnectedStatus = Some(true),
          optIndepValuationSupport = Some(false),
          movableSchedule29A = true,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = None
        ),
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Contribution,
          optDateOfAcqOrContrib = Some(localDate),
          costOfAsset = money.value,
          optPropertyAcquiredFromName = None,
          optPropertyAcquiredFrom = None,
          optConnectedStatus = None,
          optIndepValuationSupport = Some(true),
          movableSchedule29A = false,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = None
        ),
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Transfer,
          optDateOfAcqOrContrib = None,
          costOfAsset = money.value,
          optPropertyAcquiredFromName = None,
          optPropertyAcquiredFrom = None,
          optConnectedStatus = None,
          optIndepValuationSupport = None,
          movableSchedule29A = false,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = None
        )
      )
    }

    "should return transformed List with disposed other assets" in {
      val userAnswers = emptyUserAnswers
      //index-1
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(1)), Acquisition)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(1)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(1)), true)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(1)), money)
        .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, refineMV(1)), localDate)
        .unsafeSet(IndependentValuationPage(srn, refineMV(1)), false)
        .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.OtherAssetSeller), Individual)
        .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, refineMV(1)), true)
        .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, refineMV(1)), "individualSellerName")
        .unsafeSet(
          OtherAssetIndividualSellerNINumberPage(srn, refineMV(1)),
          ConditionalYesNo.no[String, Nino]("reason")
        )
        .unsafeSet(OtherAssetsDisposalPage(srn), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)), Sold)
        .unsafeSet(WhenWasAssetSoldPage(srn, refineMV(1), refineMV(1)), localDate)
        .unsafeSet(TotalConsiderationSaleAssetPage(srn, refineMV(1), refineMV(1)), money)
        .unsafeSet(TypeOfAssetBuyerPage(srn, refineMV(1), refineMV(1)), Individual)
        .unsafeSet(IndividualNameOfAssetBuyerPage(srn, refineMV(1), refineMV(1)), "individualBuyerName")
        .unsafeSet(
          AssetIndividualBuyerNiNumberPage(srn, refineMV(1), refineMV(1)),
          ConditionalYesNo.no[String, Nino]("reason")
        )
        .unsafeSet(IsBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1)), true)
        .unsafeSet(AssetSaleIndependentValuationPage(srn, refineMV(1), refineMV(1)), false)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(1), refineMV(1)), true)

        //index-2
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(2)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(2)), Contribution)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(2)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(2)), false)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(2)), money)
        .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, refineMV(2)), localDate)
        .unsafeSet(IndependentValuationPage(srn, refineMV(2)), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(2), refineMV(1)), Transferred)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(2), refineMV(1)), false)
        //index-3
        .unsafeSet(OtherAssetsCompleted(srn, refineMV(3)), SectionCompleted)
        .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(3)), "assetDescription")
        .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, refineMV(3)), Transfer)
        .unsafeSet(CostOfOtherAssetPage(srn, refineMV(3)), money)
        .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, refineMV(3)), false)
        .unsafeSet(IncomeFromAssetPage(srn, refineMV(3)), money)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(3), refineMV(1)), Other("OtherMethod"))
        .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(3), refineMV(1)), true)

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn, otherAssetDisposed = true)(request)
      result shouldMatchTo List(
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Acquisition,
          optDateOfAcqOrContrib = Some(localDate),
          costOfAsset = money.value,
          optPropertyAcquiredFromName = Some("individualSellerName"),
          optPropertyAcquiredFrom = Some(
            PropertyAcquiredFrom(
              identityType = Individual,
              idNumber = None,
              reasonNoIdNumber = Some("reason"),
              otherDescription = None
            )
          ),
          optConnectedStatus = Some(true),
          optIndepValuationSupport = Some(false),
          movableSchedule29A = true,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = Some(
            Seq(
              OtherAssetDisposed(
                methodOfDisposal = Sold.name,
                optOtherMethod = None,
                optDateSold = Some(localDate),
                optPurchaserName = Some("individualBuyerName"),
                optPropertyAcquiredFrom = Some(
                  PropertyAcquiredFrom(
                    identityType = Individual,
                    idNumber = None,
                    reasonNoIdNumber = Some("reason"),
                    otherDescription = None
                  )
                ),
                optTotalAmountReceived = Some(money.value),
                optConnectedStatus = Some(true),
                optSupportedByIndepValuation = Some(false),
                anyPartAssetStillHeld = true
              )
            )
          )
        ),
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Contribution,
          optDateOfAcqOrContrib = Some(localDate),
          costOfAsset = money.value,
          optPropertyAcquiredFromName = None,
          optPropertyAcquiredFrom = None,
          optConnectedStatus = None,
          optIndepValuationSupport = Some(true),
          movableSchedule29A = false,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = Some(
            Seq(
              OtherAssetDisposed(
                methodOfDisposal = Transferred.name,
                optOtherMethod = None,
                optDateSold = None,
                optPurchaserName = None,
                optPropertyAcquiredFrom = None,
                optTotalAmountReceived = None,
                optConnectedStatus = None,
                optSupportedByIndepValuation = None,
                anyPartAssetStillHeld = false
              )
            )
          )
        ),
        OtherAssetTransaction(
          assetDescription = "assetDescription",
          methodOfHolding = Transfer,
          optDateOfAcqOrContrib = None,
          costOfAsset = money.value,
          optPropertyAcquiredFromName = None,
          optPropertyAcquiredFrom = None,
          optConnectedStatus = None,
          optIndepValuationSupport = None,
          movableSchedule29A = false,
          totalIncomeOrReceipts = money.value,
          optOtherAssetDisposed = Some(
            Seq(
              OtherAssetDisposed(
                methodOfDisposal = Other.name,
                optOtherMethod = Some("OtherMethod"),
                optDateSold = None,
                optPurchaserName = None,
                optPropertyAcquiredFrom = None,
                optTotalAmountReceived = None,
                optConnectedStatus = None,
                optSupportedByIndepValuation = None,
                anyPartAssetStillHeld = true
              )
            )
          )
        )
      )
    }
  }

  "OtherAssetTransactionsTransformer - From Etmp" - {
    "when otherAssetTransaction Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        OtherAssets(otherAssetsWereHeld = true, otherAssetsWereDisposed = false, otherAssetTransactions = List.empty)
      )
      result.fold(ex => fail(ex.getMessage), userAnswers => userAnswers shouldMatchTo userAnswers)
    }

    "when otherAssetTransaction not Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        OtherAssets(
          otherAssetsWereHeld = true,
          otherAssetsWereDisposed = true,
          otherAssetTransactions = Seq(
            OtherAssetTransaction(
              assetDescription = "assetDescription",
              methodOfHolding = Acquisition,
              optDateOfAcqOrContrib = Some(localDate),
              costOfAsset = money.value,
              optPropertyAcquiredFromName = Some("PropertyAcquiredFromName"),
              optPropertyAcquiredFrom = Some(
                PropertyAcquiredFrom(
                  identityType = IdentityType.Other,
                  idNumber = None,
                  reasonNoIdNumber = None,
                  otherDescription = Some("otherDescription")
                )
              ),
              optConnectedStatus = Some(true),
              optIndepValuationSupport = Some(false),
              movableSchedule29A = true,
              totalIncomeOrReceipts = money.value,
              optOtherAssetDisposed = Some(
                Seq(
                  OtherAssetDisposed(
                    methodOfDisposal = Sold.name,
                    optOtherMethod = None,
                    optDateSold = Some(localDate),
                    optPurchaserName = Some("nameOfPurchaser"),
                    optPropertyAcquiredFrom = Some(
                      PropertyAcquiredFrom(
                        identityType = UKCompany,
                        idNumber = None,
                        reasonNoIdNumber = Some("optReasonNoCRN"),
                        otherDescription = None
                      )
                    ),
                    optTotalAmountReceived = Some(money.value),
                    optConnectedStatus = Some(true),
                    optSupportedByIndepValuation = Some(false),
                    anyPartAssetStillHeld = true
                  )
                )
              )
            ),
            OtherAssetTransaction(
              assetDescription = "assetDescription",
              methodOfHolding = Contribution,
              optDateOfAcqOrContrib = Some(localDate),
              costOfAsset = money.value,
              optPropertyAcquiredFromName = None,
              optPropertyAcquiredFrom = None,
              optConnectedStatus = None,
              optIndepValuationSupport = Some(true),
              movableSchedule29A = false,
              totalIncomeOrReceipts = money.value,
              optOtherAssetDisposed = Some(
                Seq(
                  OtherAssetDisposed(
                    methodOfDisposal = Transferred.name,
                    optOtherMethod = None,
                    optDateSold = None,
                    optPurchaserName = None,
                    optPropertyAcquiredFrom = None,
                    optTotalAmountReceived = None,
                    optConnectedStatus = None,
                    optSupportedByIndepValuation = None,
                    anyPartAssetStillHeld = true
                  )
                )
              )
            ),
            OtherAssetTransaction(
              assetDescription = "assetDescription",
              methodOfHolding = Transfer,
              optDateOfAcqOrContrib = None,
              costOfAsset = money.value,
              optPropertyAcquiredFromName = None,
              optPropertyAcquiredFrom = None,
              optConnectedStatus = None,
              optIndepValuationSupport = None,
              movableSchedule29A = true,
              totalIncomeOrReceipts = money.value,
              optOtherAssetDisposed = Some(
                Seq(
                  OtherAssetDisposed(
                    methodOfDisposal = Other.name,
                    optOtherMethod = Some("OtherMethod"),
                    optDateSold = None,
                    optPurchaserName = None,
                    optPropertyAcquiredFrom = None,
                    optTotalAmountReceived = None,
                    optConnectedStatus = None,
                    optSupportedByIndepValuation = None,
                    anyPartAssetStillHeld = false
                  )
                )
              )
            )
          )
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          //index-1
          userAnswers.get(OtherAssetsHeldPage(srn)) shouldMatchTo Some(true)
          userAnswers.get(WhatIsOtherAssetPage(srn, refineMV(1))) shouldMatchTo Some("assetDescription")
          userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, refineMV(1))) shouldMatchTo Some(Acquisition)
          userAnswers.get(CostOfOtherAssetPage(srn, refineMV(1))) shouldMatchTo Some(money)
          userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, refineMV(1))) shouldMatchTo Some(true)
          userAnswers.get(IncomeFromAssetPage(srn, refineMV(1))) shouldMatchTo Some(money)
          userAnswers.get(WhenDidSchemeAcquireAssetsPage(srn, refineMV(1))) shouldMatchTo Some(localDate)
          userAnswers.get(IndependentValuationPage(srn, refineMV(1))) shouldMatchTo Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.OtherAssetSeller)) shouldMatchTo Some(
            IdentityType.Other
          )
          userAnswers.get(OtherAssetSellerConnectedPartyPage(srn, refineMV(1))) shouldMatchTo Some(true)
          userAnswers
            .get(OtherRecipientDetailsPage(srn, refineMV(1), IdentitySubject.OtherAssetSeller)) shouldMatchTo Some(
            RecipientDetails(name = "PropertyAcquiredFromName", description = "otherDescription")
          )
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(1))) shouldMatchTo Some(SectionCompleted)
          userAnswers.get(OtherAssetsDisposalPage(srn)) shouldMatchTo Some(true)
          userAnswers.get(OtherAssetsDisposalCompleted(srn)) shouldMatchTo Some(SectionCompleted)
          userAnswers.get(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(Sold)
          userAnswers.get(AnyPartAssetStillHeldPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(true)
          userAnswers.get(TypeOfAssetBuyerPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(UKCompany)
          userAnswers.get(WhenWasAssetSoldPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(localDate)
          userAnswers.get(TotalConsiderationSaleAssetPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(money)
          userAnswers.get(IsBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(true)
          userAnswers.get(AssetSaleIndependentValuationPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(false)
          userAnswers.get(CompanyNameOfAssetBuyerPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(
            "nameOfPurchaser"
          )
          userAnswers.get(AssetCompanyBuyerCrnPage(srn, refineMV(1), refineMV(1))) shouldMatchTo Some(
            ConditionalYesNo.no("optReasonNoCRN")
          )

          //index-2
          userAnswers.get(WhatIsOtherAssetPage(srn, refineMV(2))) shouldMatchTo Some("assetDescription")
          userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, refineMV(2))) shouldMatchTo Some(Contribution)
          userAnswers.get(CostOfOtherAssetPage(srn, refineMV(2))) shouldMatchTo Some(money)
          userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, refineMV(2))) shouldMatchTo Some(false)
          userAnswers.get(IncomeFromAssetPage(srn, refineMV(2))) shouldMatchTo Some(money)
          userAnswers.get(WhenDidSchemeAcquireAssetsPage(srn, refineMV(2))) shouldMatchTo Some(localDate)
          userAnswers.get(IndependentValuationPage(srn, refineMV(2))) shouldMatchTo Some(true)
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(1))) shouldMatchTo Some(SectionCompleted)
          userAnswers.get(OtherAssetsDisposalProgress(srn, refineMV(2), refineMV(1))) shouldMatchTo Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWasAssetDisposedOfPage(srn, refineMV(2), refineMV(1))) shouldMatchTo Some(Transferred)
          userAnswers.get(AnyPartAssetStillHeldPage(srn, refineMV(2), refineMV(1))) shouldMatchTo Some(true)
          //index-3
          userAnswers.get(WhatIsOtherAssetPage(srn, refineMV(3))) shouldMatchTo Some("assetDescription")
          userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, refineMV(3))) shouldMatchTo Some(Transfer)
          userAnswers.get(CostOfOtherAssetPage(srn, refineMV(3))) shouldMatchTo Some(money)
          userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, refineMV(3))) shouldMatchTo Some(true)
          userAnswers.get(IncomeFromAssetPage(srn, refineMV(3))) shouldMatchTo Some(money)
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(3))) shouldMatchTo Some(SectionCompleted)
          userAnswers.get(OtherAssetsDisposalProgress(srn, refineMV(3), refineMV(1))) shouldMatchTo Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWasAssetDisposedOfPage(srn, refineMV(3), refineMV(1))) shouldMatchTo Some(
            Other("OtherMethod")
          )
          userAnswers.get(AnyPartAssetStillHeldPage(srn, refineMV(3), refineMV(1))) shouldMatchTo Some(false)
        }
      )
    }

  }
}
