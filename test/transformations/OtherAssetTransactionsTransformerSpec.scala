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

package transformations

import play.api.test.FakeRequest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.otherassetsheld._
import models.IdentityType.{Individual, Other}
import controllers.TestValues
import models.requests.psr._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import viewmodels.models.SectionCompleted
import play.api.mvc.AnyContentAsEmpty
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
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

      val result = transformer.transformToEtmp(srn = srn)
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

      val result = transformer.transformToEtmp(srn)(request)
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
          totalIncomeOrReceipts = money.value
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
          totalIncomeOrReceipts = money.value
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
          totalIncomeOrReceipts = money.value
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
          otherAssetsWereDisposed = false,
          otherAssetTransactions = Seq(
            OtherAssetTransaction(
              assetDescription = "assetDescription",
              methodOfHolding = Acquisition,
              optDateOfAcqOrContrib = Some(localDate),
              costOfAsset = money.value,
              optPropertyAcquiredFromName = Some("PropertyAcquiredFromName"),
              optPropertyAcquiredFrom = Some(
                PropertyAcquiredFrom(
                  identityType = Other,
                  idNumber = None,
                  reasonNoIdNumber = None,
                  otherDescription = Some("otherDescription")
                )
              ),
              optConnectedStatus = Some(true),
              optIndepValuationSupport = Some(false),
              movableSchedule29A = true,
              totalIncomeOrReceipts = money.value
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
              totalIncomeOrReceipts = money.value
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
              totalIncomeOrReceipts = money.value
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
            Other
          )
          userAnswers.get(OtherAssetSellerConnectedPartyPage(srn, refineMV(1))) shouldMatchTo Some(true)
          userAnswers
            .get(OtherRecipientDetailsPage(srn, refineMV(1), IdentitySubject.OtherAssetSeller)) shouldMatchTo Some(
            RecipientDetails(name = "PropertyAcquiredFromName", description = "otherDescription")
          )
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(1))) shouldMatchTo Some(SectionCompleted)

          //index-2
          userAnswers.get(WhatIsOtherAssetPage(srn, refineMV(2))) shouldMatchTo Some("assetDescription")
          userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, refineMV(2))) shouldMatchTo Some(Contribution)
          userAnswers.get(CostOfOtherAssetPage(srn, refineMV(2))) shouldMatchTo Some(money)
          userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, refineMV(2))) shouldMatchTo Some(false)
          userAnswers.get(IncomeFromAssetPage(srn, refineMV(2))) shouldMatchTo Some(money)
          userAnswers.get(WhenDidSchemeAcquireAssetsPage(srn, refineMV(2))) shouldMatchTo Some(localDate)
          userAnswers.get(IndependentValuationPage(srn, refineMV(2))) shouldMatchTo Some(true)
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(1))) shouldMatchTo Some(SectionCompleted)

          //index-3
          userAnswers.get(WhatIsOtherAssetPage(srn, refineMV(3))) shouldMatchTo Some("assetDescription")
          userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, refineMV(3))) shouldMatchTo Some(Transfer)
          userAnswers.get(CostOfOtherAssetPage(srn, refineMV(3))) shouldMatchTo Some(money)
          userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, refineMV(3))) shouldMatchTo Some(true)
          userAnswers.get(IncomeFromAssetPage(srn, refineMV(3))) shouldMatchTo Some(money)
          userAnswers.get(OtherAssetsCompleted(srn, refineMV(3))) shouldMatchTo Some(SectionCompleted)
        }
      )
    }

  }
}