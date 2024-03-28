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
import play.api.mvc.AnyContentAsEmpty
import controllers.TestValues
import models.requests.psr.{Borrowing, MoneyBorrowed}
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.moneyborrowed._
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.libs.json.Json

class MoneyBorrowedTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new MoneyBorrowedTransformer()

  "MoneyBorrowedTransformerSpec - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn)
      result mustBe List.empty
    }

    "should return empty List when index as string not a valid number" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(
          Paths.moneyBorrowed \ "borrowingFromName",
          Json.obj("InvalidIntValue" -> true)
        )

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn)(request)
      result mustBe List.empty
    }

    "should return transformed List" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LenderNamePage(srn, refineMV(1)), "borrowingFromName")
        .unsafeSet(IsLenderConnectedPartyPage(srn, refineMV(1)), true)
        .unsafeSet(BorrowedAmountAndRatePage(srn, refineMV(1)), (money, percentage))
        .unsafeSet(WhenBorrowedPage(srn, refineMV(1)), localDate)
        .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, refineMV(1)), money)
        .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), "reasonForBorrow")

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn)(request)
      result mustBe List(
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
    }
  }

  "MoneyBorrowedTransformerSpec - From Etmp" - {
    "should transform successfully" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        Borrowing(
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
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(MoneyBorrowedPage(srn)) mustBe Some(true)
          userAnswers.get(WhenBorrowedPage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(BorrowedAmountAndRatePage(srn, refineMV(1))) mustBe Some((money, percentage))
          userAnswers.get(LenderNamePage(srn, refineMV(1))) mustBe Some("LenderName")
          userAnswers.get(WhySchemeBorrowedMoneyPage(srn, refineMV(1))) mustBe Some("reasonForBorrow")
          userAnswers.get(IsLenderConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, refineMV(1))) mustBe Some(money)
        }
      )
    }
  }
}
