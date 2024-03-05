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

import controllers.TestValues
import eu.timepit.refined.refineMV
import generators.ModelGenerators.allowedAccessRequestGen
import models.SchemeHoldBond.{Acquisition, Contribution}
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.unregulatedorconnectedbonds._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.SectionCompleted

class BondTransactionsTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new BondTransactionsTransformer

  "BondTransactionsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn = srn)
      result mustBe List.empty
    }

    "should return transformed List" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "nameOfBonds")
        .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, refineMV(1)), Acquisition)
        .unsafeSet(CostOfBondsPage(srn, refineMV(1)), money)
        .unsafeSet(AreBondsUnregulatedPage(srn, refineMV(1)), true)
        .unsafeSet(IncomeFromBondsPage(srn, refineMV(1)), money)
        .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, refineMV(1)), localDate)
        .unsafeSet(BondsFromConnectedPartyPage(srn, refineMV(1)), false)

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn)(request)
      result mustBe List(
        BondTransactions(
          nameOfBonds = "nameOfBonds",
          methodOfHolding = Acquisition,
          optDateOfAcqOrContrib = Some(localDate),
          costOfBonds = money.value,
          optConnectedPartyStatus = Some(false),
          bondsUnregulated = true,
          totalIncomeOrReceipts = money.value
        )
      )
    }
  }

  "BondTransactionsTransformer - From Etmp" - {
    "when bondTransactions Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Bonds(bondsWereAdded = true, bondsWereDisposed = true, bondTransactions = Seq.empty)
      )
      result.fold(ex => fail(ex.getMessage), userAnswers => userAnswers mustBe userAnswers)
    }

    "when bondTransactions not Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Bonds(
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
              totalIncomeOrReceipts = money.value
            )
          )
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn)) mustBe Some(true)
          userAnswers.get(NameOfBondsPage(srn, refineMV(1))) mustBe Some("nameOfBonds")
          userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, refineMV(1))) mustBe Some(Contribution)
          userAnswers.get(CostOfBondsPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(AreBondsUnregulatedPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(BondsFromConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(BondsCompleted(srn, refineMV(1))) mustBe Some(SectionCompleted)
        }
      )
    }

  }
}
