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

import com.google.inject.Singleton
import config.RefinedTypes.OneTo5000
import eu.timepit.refined.refineV
import models.{Money, Percentage, UserAnswers}
import pages.nonsipp.moneyborrowed._
import viewmodels.models.SectionJourneyStatus
import models.requests.DataRequest
import pages.nonsipp.moneyborrowed.Paths.borrowing
import models.SchemeId.Srn
import models.requests.psr._
import models.UserAnswers.implicits.UserAnswersTryOps

import scala.util.Try

import javax.inject.Inject

@Singleton()
class BorrowingTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, optMoneyWasBorrowed: Option[Boolean], initialUA: UserAnswers)(implicit
    request: DataRequest[?]
  ): Option[Borrowing] =
    if (
      optMoneyWasBorrowed.isEmpty ||
      (
        optMoneyWasBorrowed.getOrElse(false) && !request.userAnswers
          .map(MoneyBorrowedProgress.all(srn))
          .toList
          .exists(_._2.completed)
      )
    ) {
      None
    } else {
      optMoneyWasBorrowed.map(moneyWasBorrowed =>
        Borrowing(
          recordVersion = Option.when(request.userAnswers.get(borrowing) == initialUA.get(borrowing))(
            request.userAnswers.get(BorrowingRecordVersionPage(srn)).get
          ),
          moneyWasBorrowed = moneyWasBorrowed,
          moneyBorrowed = moneyBorrowedTransformToEtmp(srn)
        )
      )
    }

  private def moneyBorrowedTransformToEtmp(srn: Srn)(implicit request: DataRequest[?]): List[MoneyBorrowed] =
    request.userAnswers
      .get(MoneyBorrowedProgress.all(srn))
      .map { value =>
        value
          .filter(_._2.completed)
          .keys
          .toList
          .flatMap { key =>
            key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
              case None => None
              case Some(index) =>
                for {
                  lenderName <- request.userAnswers.get(LenderNamePage(srn, index))
                  isLenderConnectedParty <- request.userAnswers.get(IsLenderConnectedPartyPage(srn, index))
                  borrowedAmountAndRate <- request.userAnswers.get(BorrowedAmountAndRatePage(srn, index))
                  whenBorrowed <- request.userAnswers.get(WhenBorrowedPage(srn, index))
                  valueOfSchemeAssetsWhenMoneyBorrowed <- request.userAnswers.get(
                    ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index)
                  )
                  whySchemeBorrowedMoney <- request.userAnswers.get(WhySchemeBorrowedMoneyPage(srn, index))
                } yield MoneyBorrowed(
                  dateOfBorrow = whenBorrowed,
                  schemeAssetsValue = valueOfSchemeAssetsWhenMoneyBorrowed.value,
                  amountBorrowed = borrowedAmountAndRate._1.value,
                  interestRate = borrowedAmountAndRate._2.value,
                  borrowingFromName = lenderName,
                  connectedPartyStatus = isLenderConnectedParty,
                  reasonForBorrow = whySchemeBorrowedMoney
                )
            }
          }
      }
      .getOrElse(List.empty)

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, borrowing: Borrowing): Try[UserAnswers] = {

    val initialUserAnswersForBorrowing = userAnswers.set(MoneyBorrowedPage(srn), borrowing.moneyWasBorrowed)
    val userAnswersWithRecordVersion =
      borrowing.recordVersion.fold(initialUserAnswersForBorrowing)(
        initialUserAnswersForBorrowing.set(BorrowingRecordVersionPage(srn), _)
      )

    val moneyBorrowedList = borrowing.moneyBorrowed

    for {
      indexes <- buildIndexesForMax5000(moneyBorrowedList.size)
      resultUA <- indexes.foldLeft(userAnswersWithRecordVersion) { case (ua, index) =>
        val moneyBorrowed = moneyBorrowedList(index.value - 1)
        val whenBorrowed = WhenBorrowedPage(srn, index) -> moneyBorrowed.dateOfBorrow
        val borrowedAmountAndRate =
          BorrowedAmountAndRatePage(srn, index) -> (Money(moneyBorrowed.amountBorrowed), Percentage(
            moneyBorrowed.interestRate
          ))
        val lenderName = LenderNamePage(srn, index) -> moneyBorrowed.borrowingFromName
        val whySchemeBorrowedMoney = WhySchemeBorrowedMoneyPage(srn, index) -> moneyBorrowed.reasonForBorrow
        val isLenderConnectedParty = IsLenderConnectedPartyPage(srn, index) -> moneyBorrowed.connectedPartyStatus
        val valueOfSchemeAssetsWhenMoneyBorrowed = ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index) -> Money(
          moneyBorrowed.schemeAssetsValue
        )

        for {
          ua0 <- ua
          ua1 <- ua0.set(whenBorrowed._1, whenBorrowed._2)
          ua2 <- ua1.set(borrowedAmountAndRate._1, borrowedAmountAndRate._2)
          ua3 <- ua2.set(lenderName._1, lenderName._2)
          ua4 <- ua3.set(whySchemeBorrowedMoney._1, whySchemeBorrowedMoney._2)
          ua5 <- ua4.set(isLenderConnectedParty._1, isLenderConnectedParty._2)
          ua6 <- ua5.set(valueOfSchemeAssetsWhenMoneyBorrowed._1, valueOfSchemeAssetsWhenMoneyBorrowed._2)
          ua7 <- ua6.set(MoneyBorrowedProgress(srn, index), SectionJourneyStatus.Completed)
        } yield ua7
      }

    } yield resultUA
  }

}
