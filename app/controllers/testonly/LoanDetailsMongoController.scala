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

import models.ConditionalYesNo._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import cats.implicits._
import uk.gov.hmrc.domain.Nino
import repositories.SessionRepository
import models._
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import java.time.LocalDate
import javax.inject.Inject

class LoanDetailsMongoController @Inject()(
  sessionRepository: SessionRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max5000 = refineMV(5000)

  def addLoanDetails(srn: Srn, num: Max5000): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllLoanDetails(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswersWithLoanDetails(num.value, srn, removedUserAnswers))
        _ <- sessionRepository.set(updatedUserAnswers)
      } yield Ok(s"Added ${num.value} loan details to UserAnswers")
  }

  private def buildIndexes(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllLoanDetails(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          ua.flatMap(_.remove(LoansMadeOrOutstandingPage(srn)))
            .flatMap(_.remove(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)))
            .flatMap(_.remove(IndividualRecipientNamePage(srn, index)))
            .flatMap(_.remove(IndividualRecipientNinoPage(srn, index)))
            .flatMap(_.remove(IsIndividualRecipientConnectedPartyPage(srn, index)))
            .flatMap(_.remove(DatePeriodLoanPage(srn, index)))
            .flatMap(_.remove(AmountOfTheLoanPage(srn, index)))
            .flatMap(_.remove(AreRepaymentsInstalmentsPage(srn, index)))
            .flatMap(_.remove(InterestOnLoanPage(srn, index)))
            .flatMap(_.remove(SecurityGivenForLoanPage(srn, index)))
            .flatMap(_.remove(OutstandingArrearsOnLoanPage(srn, index)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithLoanDetails(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      schemeHadLoans = indexes.map(_ => LoansMadeOrOutstandingPage(srn) -> true)
      identityTypes = indexes.map(
        index => IdentityTypePage(srn, index, IdentitySubject.LoanRecipient) -> IdentityType.Individual
      )
      loanRecipientName = indexes
        .map(index => IndividualRecipientNamePage(srn, index) -> buildRandomNameDOB().fullName)
        .sortBy(_._1.index.value)
      nino = indexes.map(
        index => IndividualRecipientNinoPage(srn, index) -> ConditionalYesNo.yes[String, Nino](Nino("SX123456A"))
      )
      individualConnectedPartyStatus = indexes.map(index => IsIndividualRecipientConnectedPartyPage(srn, index) -> true)
      datePeriodLoan = indexes.map(
        index => DatePeriodLoanPage(srn, index) -> (LocalDate.of(1990, 12, 12), Money(121.00, "121.00"), 12)
      )
      loanAmount = indexes.map(
        index =>
          AmountOfTheLoanPage(srn, index) -> AmountOfTheLoan(
            Money(12.00, "12.00"),
            Some(Money(12.00, "12.00")),
            Some(Money(12.00, "12.00"))
          )
      )
      equalInstallments = indexes.map(index => AreRepaymentsInstalmentsPage(srn, index) -> true)
      loanInterest = indexes.map(
        index =>
          InterestOnLoanPage(srn, index) -> InterestOnLoan(
            Money(12.00, "12.00"),
            Percentage(12.00, "12.00"),
            Some(Money(12.00, "12.00"))
          )
      )
      securityGiven = indexes.map(
        index =>
          SecurityGivenForLoanPage(srn, index) -> ConditionalYesNo.yes[Unit, Security](Security("securityGivenForLoan"))
      )
      outstandingArrearsOnLoan = indexes.map(
        index =>
          OutstandingArrearsOnLoanPage(srn, index) -> ConditionalYesNo.yes[Unit, Money](Money(1234.00, "1,234.00"))
      )

      ua1 <- schemeHadLoans.foldLeft(Try(userAnswers)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua2 <- identityTypes.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- loanRecipientName.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua4 <- nino.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua5 <- individualConnectedPartyStatus.foldLeft(Try(ua4)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua6 <- datePeriodLoan.foldLeft(Try(ua5)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua7 <- loanAmount.foldLeft(Try(ua6)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua8 <- equalInstallments.foldLeft(Try(ua7)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua9 <- loanInterest.foldLeft(Try(ua8)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua10 <- securityGiven.foldLeft(Try(ua9)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua11 <- outstandingArrearsOnLoan.foldLeft(Try(ua10)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
    } yield ua11

}
