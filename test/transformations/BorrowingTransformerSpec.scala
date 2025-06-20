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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import controllers.TestValues
import utils.IntUtils.given
import models.requests.psr.{Borrowing, MoneyBorrowed}
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.moneyborrowed._
import viewmodels.models.SectionJourneyStatus
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.libs.json.Json

class BorrowingTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new BorrowingTransformer()

  "BorrowingTransformer - To Etmp" - {
    "should return None when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, None, defaultUserAnswers)
      result mustBe None
    }

    "should omit Record Version when there is a change in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), false)
        .unsafeSet(MoneyBorrowedProgress(srn, 1), SectionJourneyStatus.Completed)
        .unsafeSet(BorrowingRecordVersionPage(srn), "001")

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), true)
        .unsafeSet(MoneyBorrowedProgress(srn, 1), SectionJourneyStatus.Completed)
        .unsafeSet(BorrowingRecordVersionPage(srn), "001")

      val result =
        transformer.transformToEtmp(srn = srn, Some(false), initialUserAnswer)(using
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe Some(Borrowing(recordVersion = None, moneyWasBorrowed = false, moneyBorrowed = Seq.empty))
    }

    "should return None when MoneyBorrowedPage is true but there is no MoneyBorrowedProgress" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), true)
        .unsafeSet(LenderNamePage(srn, 1), "borrowingFromName")

      val result =
        transformer.transformToEtmp(srn = srn, Some(true), userAnswers)(using
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe None
    }

    "should return Some when MoneyBorrowedPage is false but there is no MoneyBorrowedProgress" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), false)
        .unsafeSet(BorrowingRecordVersionPage(srn), "001")

      val result =
        transformer.transformToEtmp(srn = srn, Some(false), userAnswers)(using
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe Some(Borrowing(recordVersion = Some("001"), moneyWasBorrowed = false, moneyBorrowed = Seq.empty))
    }

    "should return recordVersion when there is no change among UAs" - {
      "should return empty List when index as string not a valid number" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(BorrowingRecordVersionPage(srn), "001")
          .unsafeSet(MoneyBorrowedProgress(srn, 1), SectionJourneyStatus.Completed)
          .unsafeSet(
            Paths.moneyBorrowed \ "borrowingFromName",
            Json.obj("InvalidIntValue" -> true)
          )

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(using request)
        result mustBe Some(Borrowing(recordVersion = Some("001"), moneyWasBorrowed = true, moneyBorrowed = Seq.empty))
      }

      "should return transformed List" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(BorrowingRecordVersionPage(srn), "001")
          .unsafeSet(LenderNamePage(srn, 1), "borrowingFromName")
          .unsafeSet(IsLenderConnectedPartyPage(srn, 1), true)
          .unsafeSet(BorrowedAmountAndRatePage(srn, 1), (money, percentage))
          .unsafeSet(WhenBorrowedPage(srn, 1), localDate)
          .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, 1), money)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, 1), "reasonForBorrow")
          .unsafeSet(MoneyBorrowedProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(using request)
        result mustBe Some(
          Borrowing(
            recordVersion = Some("001"),
            moneyWasBorrowed = true,
            moneyBorrowed = List(
              MoneyBorrowed(
                dateOfBorrow = localDate,
                schemeAssetsValue = money.value,
                amountBorrowed = money.value,
                interestRate = percentage.value,
                borrowingFromName = "borrowingFromName",
                connectedPartyStatus = true,
                reasonForBorrow = "reasonForBorrow"
              )
            )
          )
        )
      }

      "should not include incomplete record" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(BorrowingRecordVersionPage(srn), "001")
          .unsafeSet(LenderNamePage(srn, 1), "borrowingFromName")
          .unsafeSet(IsLenderConnectedPartyPage(srn, 1), true)
          .unsafeSet(BorrowedAmountAndRatePage(srn, 1), (money, percentage))
          .unsafeSet(WhenBorrowedPage(srn, 1), localDate)
          .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, 1), money)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, 1), "reasonForBorrow")
          .unsafeSet(MoneyBorrowedProgress(srn, 1), SectionJourneyStatus.Completed)
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(BorrowingRecordVersionPage(srn), "001")
          .unsafeSet(LenderNamePage(srn, 2), "borrowingFromName")
          .unsafeSet(IsLenderConnectedPartyPage(srn, 2), true)
          .unsafeSet(BorrowedAmountAndRatePage(srn, 2), (money, percentage))
          .unsafeSet(WhenBorrowedPage(srn, 2), localDate)
          .unsafeSet(
            MoneyBorrowedProgress(srn, 2),
            SectionJourneyStatus.InProgress(
              "someurl"
            )
          )

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(using request)
        result mustBe Some(
          Borrowing(
            recordVersion = Some("001"),
            moneyWasBorrowed = true,
            moneyBorrowed = List(
              MoneyBorrowed(
                dateOfBorrow = localDate,
                schemeAssetsValue = money.value,
                amountBorrowed = money.value,
                interestRate = percentage.value,
                borrowingFromName = "borrowingFromName",
                connectedPartyStatus = true,
                reasonForBorrow = "reasonForBorrow"
              )
            )
          )
        )
      }
    }
  }

  "BorrowingTransformer - From Etmp" - {

    "when only recordVersion available" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Borrowing(
          recordVersion = Some("001"),
          moneyWasBorrowed = false,
          moneyBorrowed = Seq.empty
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(MoneyBorrowedPage(srn)) mustBe Some(false)
          userAnswers.get(BorrowingRecordVersionPage(srn)) mustBe Some("001")
        }
      )
    }

    "should transform successfully" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        Borrowing(
          recordVersion = Some("001"),
          moneyWasBorrowed = true,
          moneyBorrowed = Seq(
            MoneyBorrowed(
              dateOfBorrow = localDate,
              schemeAssetsValue = money.value,
              amountBorrowed = money.value,
              interestRate = percentage.value,
              borrowingFromName = "LenderName",
              connectedPartyStatus = true,
              reasonForBorrow = "reasonForBorrow"
            )
          )
        )
      )

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(MoneyBorrowedPage(srn)) mustBe Some(true)
          userAnswers.get(BorrowingRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(WhenBorrowedPage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(BorrowedAmountAndRatePage(srn, 1)) mustBe Some((money, percentage))
          userAnswers.get(LenderNamePage(srn, 1)) mustBe Some("LenderName")
          userAnswers.get(WhySchemeBorrowedMoneyPage(srn, 1)) mustBe Some("reasonForBorrow")
          userAnswers.get(IsLenderConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, 1)) mustBe Some(money)
        }
      )
    }
  }
}
