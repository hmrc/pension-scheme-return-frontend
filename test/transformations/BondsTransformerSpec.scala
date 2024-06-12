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
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import models.HowDisposed.{Other, Sold, Transferred}
import models.requests.psr._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import org.scalatest.OptionValues
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionCompleted
import models.requests.{AllowedAccessRequest, DataRequest}
import pages.nonsipp.bonds._
import org.scalatest.freespec.AnyFreeSpec
import controllers.TestValues
import models.SchemeHoldBond.{Acquisition, Contribution}

class BondsTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new BondsTransformer

  "BondsTransformer - To Etmp" - {
    "should return None when userAnswer is empty" in {
      val result = transformer.transformToEtmp(srn = srn, None, defaultUserAnswers)
      result mustBe None
    }

    "should omit Record Version when there is a change in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)
        .unsafeSet(BondsRecordVersionPage(srn), "001")

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
        .unsafeSet(BondsRecordVersionPage(srn), "001")

      val result =
        transformer.transformToEtmp(srn = srn, Some(false), initialUserAnswer)(
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe Some(
        Bonds(recordVersion = None, bondsWereAdded = false, bondsWereDisposed = false, bondTransactions = Seq.empty)
      )
    }

    "should return recordVersion when there is no change among UAs" - {
      "should return transformed List without disposed bonds" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsRecordVersionPage(srn), "001")
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "nameOfBonds")
          .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, refineMV(1)), Acquisition)
          .unsafeSet(CostOfBondsPage(srn, refineMV(1)), money)
          .unsafeSet(AreBondsUnregulatedPage(srn, refineMV(1)), true)
          .unsafeSet(IncomeFromBondsPage(srn, refineMV(1)), money)
          .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, refineMV(1)), localDate)
          .unsafeSet(BondsFromConnectedPartyPage(srn, refineMV(1)), false)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(request)
        result mustBe Some(
          Bonds(
            recordVersion = Some("001"),
            bondsWereAdded = true,
            bondsWereDisposed = false,
            bondTransactions = List(
              BondTransactions(
                nameOfBonds = "nameOfBonds",
                methodOfHolding = Acquisition,
                optDateOfAcqOrContrib = Some(localDate),
                costOfBonds = money.value,
                optConnectedPartyStatus = Some(false),
                bondsUnregulated = true,
                totalIncomeOrReceipts = money.value,
                optBondsDisposed = None
              )
            )
          )
        )
      }

      "should return transformed List with disposed bonds" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsDisposalPage(srn), true)
          .unsafeSet(BondsRecordVersionPage(srn), "001")
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "nameOfBonds")
          .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, refineMV(1)), Acquisition)
          .unsafeSet(CostOfBondsPage(srn, refineMV(1)), money)
          .unsafeSet(AreBondsUnregulatedPage(srn, refineMV(1)), true)
          .unsafeSet(IncomeFromBondsPage(srn, refineMV(1)), money)
          .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, refineMV(1)), localDate)
          .unsafeSet(BondsFromConnectedPartyPage(srn, refineMV(1)), false)
          .unsafeSet(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(1)), Sold)
          .unsafeSet(BondsStillHeldPage(srn, refineMV(1), refineMV(1)), 1)
          .unsafeSet(WhenWereBondsSoldPage(srn, refineMV(1), refineMV(1)), localDate)
          .unsafeSet(TotalConsiderationSaleBondsPage(srn, refineMV(1), refineMV(1)), money)
          .unsafeSet(BuyerNamePage(srn, refineMV(1), refineMV(1)), "BuyerName")
          .unsafeSet(IsBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1)), false)
          .unsafeSet(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(2)), Other("OtherMethod"))
          .unsafeSet(BondsStillHeldPage(srn, refineMV(1), refineMV(2)), 2)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(request)
        result mustBe Some(
          Bonds(
            recordVersion = Some("001"),
            bondsWereAdded = true,
            bondsWereDisposed = true,
            bondTransactions = List(
              BondTransactions(
                nameOfBonds = "nameOfBonds",
                methodOfHolding = Acquisition,
                optDateOfAcqOrContrib = Some(localDate),
                costOfBonds = money.value,
                optConnectedPartyStatus = Some(false),
                bondsUnregulated = true,
                totalIncomeOrReceipts = money.value,
                optBondsDisposed = Some(
                  Seq(
                    BondDisposed(
                      methodOfDisposal = Sold.name,
                      optOtherMethod = None,
                      optDateSold = Some(localDate),
                      optAmountReceived = Some(money.value),
                      optBondsPurchaserName = Some("BuyerName"),
                      optConnectedPartyStatus = Some(false),
                      totalNowHeld = 1
                    ),
                    BondDisposed(
                      methodOfDisposal = Other.name,
                      optOtherMethod = Some("OtherMethod"),
                      optDateSold = None,
                      optAmountReceived = None,
                      optBondsPurchaserName = None,
                      optConnectedPartyStatus = None,
                      totalNowHeld = 2
                    )
                  )
                )
              )
            )
          )
        )
      }
    }
  }

  "BondsTransformer - From Etmp" - {
    "when only recordVersion available" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Bonds(
          recordVersion = Some("001"),
          bondsWereAdded = false,
          bondsWereDisposed = false,
          bondTransactions = Seq.empty
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(BondsRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn)) mustBe Some(false)
        }
      )
    }

    "when bondTransactions not Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Bonds(
          recordVersion = Some("001"),
          bondsWereAdded = true,
          bondsWereDisposed = true,
          bondTransactions = Seq(
            BondTransactions(
              nameOfBonds = "nameOfBonds",
              methodOfHolding = Contribution,
              optDateOfAcqOrContrib = Some(localDate),
              costOfBonds = money.value,
              optConnectedPartyStatus = Some(true),
              bondsUnregulated = false,
              totalIncomeOrReceipts = money.value,
              optBondsDisposed = Some(
                Seq(
                  BondDisposed(
                    methodOfDisposal = Sold.name,
                    optOtherMethod = None,
                    optDateSold = Some(localDate),
                    optAmountReceived = Some(money.value),
                    optBondsPurchaserName = Some("BondsPurchaserName"),
                    optConnectedPartyStatus = Some(true),
                    totalNowHeld = 1
                  ),
                  BondDisposed(
                    methodOfDisposal = Transferred.name,
                    optOtherMethod = None,
                    optDateSold = None,
                    optAmountReceived = None,
                    optBondsPurchaserName = None,
                    optConnectedPartyStatus = None,
                    totalNowHeld = 2
                  ),
                  BondDisposed(
                    methodOfDisposal = Other.name,
                    optOtherMethod = Some("OtherMethod"),
                    optDateSold = None,
                    optAmountReceived = None,
                    optBondsPurchaserName = None,
                    optConnectedPartyStatus = None,
                    totalNowHeld = 3
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
          userAnswers.get(BondsRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn)) mustBe Some(true)
          userAnswers.get(BondsDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(NameOfBondsPage(srn, refineMV(1))) mustBe Some("nameOfBonds")
          userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, refineMV(1))) mustBe Some(Contribution)
          userAnswers.get(CostOfBondsPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(AreBondsUnregulatedPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(BondsFromConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(BondsCompleted(srn, refineMV(1))) mustBe Some(SectionCompleted)
          userAnswers.get(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(BondsStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(1)
          userAnswers.get(WhenWereBondsSoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(TotalConsiderationSaleBondsPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(BuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some("BondsPurchaserName")
          userAnswers.get(IsBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(2))) mustBe Some(Transferred)
          userAnswers.get(BondsStillHeldPage(srn, refineMV(1), refineMV(2))) mustBe Some(2)
          userAnswers.get(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(3))) mustBe Some(Other("OtherMethod"))
          userAnswers.get(BondsStillHeldPage(srn, refineMV(1), refineMV(3))) mustBe Some(3)
        }
      )
    }

  }
}
