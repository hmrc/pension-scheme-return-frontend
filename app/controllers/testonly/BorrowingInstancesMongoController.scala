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

package controllers.testonly

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max5000, OneTo5000}
import models.SchemeId.Srn
import cats.implicits._
import repositories.SessionRepository
import models.{Money, Percentage, UserAnswers}
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.{refineMV, refineV}
import play.api.i18n.I18nSupport
import pages.nonsipp.moneyborrowed._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import java.time.LocalDate
import javax.inject.Inject

class BorrowingInstancesMongoController @Inject()(
  sessionRepository: SessionRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max5000 = refineMV(5000)

  def addBorrowDetails(srn: Srn, num: Max5000): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllBorrowDetails(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswersWithBorrowDetails(num.value, srn, removedUserAnswers))
        _ <- sessionRepository.set(updatedUserAnswers)
      } yield Ok(s"Added ${num.value} of borrow instances to UserAnswers")
  }

  private def buildIndexes(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllBorrowDetails(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          ua.flatMap(_.remove(LenderNamePage(srn, index)))
            .flatMap(_.remove(IsLenderConnectedPartyPage(srn, index)))
            .flatMap(_.remove(BorrowedAmountAndRatePage(srn, index)))
            .flatMap(_.remove(WhenBorrowedPage(srn, index)))
            .flatMap(_.remove(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index)))
            .flatMap(_.remove(WhySchemeBorrowedMoneyPage(srn, index)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithBorrowDetails(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      schemeHadBorrow = indexes.map(_ => MoneyBorrowedPage(srn) -> true)

      lenderName = indexes
        .map(index => LenderNamePage(srn, index) -> s"Name-$index")
        .sortBy(_._1.index.value)

      isLenderConnectedParty = indexes.map(index => IsLenderConnectedPartyPage(srn, index) -> true)

      borrowedAmount = indexes.map(
        index => BorrowedAmountAndRatePage(srn, index) -> (Money(12.00, "12.00"), Percentage(2, "2"))
      )

      whenBorrowed = indexes.map(
        index => WhenBorrowedPage(srn, index) -> LocalDate.of(2000, 12, 12)
      )

      totalSchemeValue = indexes.map(
        index => ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index) -> Money(12.00, "12.00")
      )

      whyBorrowed = indexes.map(
        index => WhySchemeBorrowedMoneyPage(srn, index) -> "Business"
      )

      ua1 <- schemeHadBorrow.foldLeft(Try(userAnswers)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua2 <- lenderName.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- isLenderConnectedParty.foldLeft(Try(ua2)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua4 <- borrowedAmount.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua5 <- whenBorrowed.foldLeft(Try(ua4)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua6 <- totalSchemeValue.foldLeft(Try(ua5)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua7 <- whyBorrowed.foldLeft(Try(ua6)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }

    } yield ua7

}
