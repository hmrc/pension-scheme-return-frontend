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

import com.google.inject.Singleton
import config.Refined.OneTo5000
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr._
import models.{Money, Percentage, UserAnswers}
import pages.nonsipp.moneyborrowed._

import javax.inject.Inject
import scala.util.Try

@Singleton()
class MoneyBorrowedTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn)(implicit request: DataRequest[_]): List[MoneyBorrowed] =
    request.userAnswers
      .get(LenderNamePages(srn))
      .map { value =>
        value.keys.toList.flatMap { key =>
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
              } yield {
                MoneyBorrowed(
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
      }
      .getOrElse(List.empty)

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, borrowing: Borrowing): Try[UserAnswers] = {
    val moneyBorrowedList = borrowing.moneyBorrowed
    for {
      indexes <- buildIndexesForMax5000(moneyBorrowedList.size)
      resultUA <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          val moneyBorrowed = moneyBorrowedList(index.value - 1)
          val whenBorrowed = WhenBorrowedPage(srn, index) -> moneyBorrowed.dateOfBorrow
          val borrowedAmountAndRate = BorrowedAmountAndRatePage(srn, index) -> (Money(moneyBorrowed.amountBorrowed), Percentage(
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
            ua1 <- ua0.set(MoneyBorrowedPage(srn), true)
            ua2 <- ua1.set(whenBorrowed._1, whenBorrowed._2)
            ua3 <- ua2.set(borrowedAmountAndRate._1, borrowedAmountAndRate._2)
            ua4 <- ua3.set(lenderName._1, lenderName._2)
            ua5 <- ua4.set(whySchemeBorrowedMoney._1, whySchemeBorrowedMoney._2)
            ua6 <- ua5.set(isLenderConnectedParty._1, isLenderConnectedParty._2)
            ua7 <- ua6.set(valueOfSchemeAssetsWhenMoneyBorrowed._1, valueOfSchemeAssetsWhenMoneyBorrowed._2)

          } yield ua7
      }

    } yield resultUA
  }

}
