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
import models.IdentityType.reads
import config.RefinedTypes.OneTo5000
import models.SchemeId.Srn
import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple3Semigroupal}
import uk.gov.hmrc.domain.Nino
import models._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import pages.nonsipp.loansmadeoroutstanding.Paths.loans
import models.ConditionalYesNo._
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import models.requests.psr._
import models.RecipientDetails.format
import eu.timepit.refined.refineV
import models.SponsoringOrConnectedParty.ConnectedParty
import models.requests.DataRequest

import scala.util.Try

import javax.inject.Inject

@Singleton()
class LoansTransformer @Inject() extends Transformer {

  private type OptionalRecipientDetails = Option[(String, RecipientIdentityType, Boolean, Option[String])]

  def transformToEtmp(srn: Srn, initialUA: UserAnswers)(implicit request: DataRequest[_]): Option[Loans] = {
    val optSchemeHadLoans = request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    if (optSchemeHadLoans.nonEmpty || isPrePopulation) {
      Some(
        Loans(
          Option
            .when(request.userAnswers.get(loans) == initialUA.get(loans))(
              request.userAnswers.get(LoansRecordVersionPage(srn))
            )
            .flatten,
          optSchemeHadLoans,
          request.userAnswers
            .map(IdentityTypes(srn, IdentitySubject.LoanRecipient))
            .map {
              case (key, identityType) =>
                key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
                  case None => None
                  case Some(index) =>
                    val loanProgress = request.userAnswers.get(LoansProgress(srn, index))
                    if (loanProgress.contains(SectionJourneyStatus.Completed)) {

                      val optRecipientIdentityDetails: OptionalRecipientDetails = identityType match {
                        case IdentityType.Individual =>
                          (
                            request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
                            request.userAnswers.get(IndividualRecipientNinoPage(srn, index)).map(_.value),
                            request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index))
                          ).mapN {
                            case (name, Left(noNinoReason), connectedParty) =>
                              (
                                name,
                                RecipientIdentityType(IdentityType.Individual, None, Some(noNinoReason), None),
                                connectedParty,
                                None
                              )
                            case (name, Right(nino), connectedParty) =>
                              (
                                name,
                                RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
                                connectedParty,
                                None
                              )
                          }
                        case IdentityType.UKCompany =>
                          val recipientSponsoringEmployer =
                            request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                          (
                            request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
                            request.userAnswers
                              .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
                              .map(_.value)
                          ).mapN {
                            case (name, Left(noCrnReason)) =>
                              (
                                name,
                                RecipientIdentityType(IdentityType.UKCompany, None, Some(noCrnReason), None),
                                recipientSponsoringEmployer.contains(ConnectedParty),
                                recipientSponsoringEmployer.map(_.name)
                              )
                            case (name, Right(crn)) =>
                              (
                                name,
                                RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
                                recipientSponsoringEmployer.contains(ConnectedParty),
                                recipientSponsoringEmployer.map(_.name)
                              )
                          }
                        case IdentityType.UKPartnership =>
                          val recipientSponsoringEmployer =
                            request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                          (
                            request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
                            request.userAnswers
                              .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
                              .map(_.value)
                          ).mapN {
                            case (name, Left(noUtrReason)) =>
                              (
                                name,
                                RecipientIdentityType(IdentityType.UKPartnership, None, Some(noUtrReason), None),
                                recipientSponsoringEmployer.contains(ConnectedParty),
                                recipientSponsoringEmployer.map(_.name)
                              )
                            case (name, Right(utr)) =>
                              (
                                name,
                                RecipientIdentityType(
                                  IdentityType.UKPartnership,
                                  Some(utr.value.filterNot(_.isWhitespace)),
                                  None,
                                  None
                                ),
                                recipientSponsoringEmployer.contains(ConnectedParty),
                                recipientSponsoringEmployer.map(_.name)
                              )
                          }
                        case IdentityType.Other =>
                          val recipientSponsoringEmployer =
                            request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                          request.userAnswers
                            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
                            .map(
                              other =>
                                (
                                  other.name,
                                  RecipientIdentityType(IdentityType.Other, None, None, Some(other.description)),
                                  recipientSponsoringEmployer.contains(ConnectedParty),
                                  recipientSponsoringEmployer.map(_.name)
                                )
                            )
                      }

                      for {
                        recipientIdentityDetails <- optRecipientIdentityDetails
                        (recipientName, recipientIdentityType, connectedParty, optRecipientSponsoringEmployer) = recipientIdentityDetails
                        equalInstallments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index))
                        datePeriodLoanDetails <- request.userAnswers.get(DatePeriodLoanPage(srn, index))
                        amountOfTheLoan <- request.userAnswers.get(AmountOfTheLoanPage(srn, index))
                        (loanAmount, optCapRepaymentCY, optAmountOutstanding) = amountOfTheLoan.asTuple
                        optSecurity <- request.userAnswers
                          .get(SecurityGivenForLoanPage(srn, index))
                          .map(_.value.toOption)
                        interestOnLoan <- request.userAnswers.get(InterestOnLoanPage(srn, index))
                        (loanInterestAmount, loanInterestRate, optIntReceivedCY) = interestOnLoan.asTuple
                        optArrearsPrevYears = request.userAnswers.get(ArrearsPrevYears(srn, index))
                        optOutstandingArrearsOnLoan = if (optArrearsPrevYears.isEmpty) {
                          None
                        } else {
                          request.userAnswers
                            .get(OutstandingArrearsOnLoanPage(srn, index))
                            .map(_.value)
                            .get
                            .map(_.value)
                            .toOption
                        }
                      } yield {
                        LoanTransactions(
                          recipientIdentityType,
                          recipientName,
                          connectedParty,
                          optRecipientSponsoringEmployer,
                          LoanPeriod(
                            datePeriodLoanDetails._1,
                            datePeriodLoanDetails._2.value,
                            datePeriodLoanDetails._3
                          ),
                          LoanAmountDetails(
                            loanAmount.value,
                            optCapRepaymentCY.map(_.value),
                            optAmountOutstanding.map(_.value)
                          ),
                          equalInstallments,
                          LoanInterestDetails(
                            loanInterestAmount.value,
                            loanInterestRate.value,
                            optIntReceivedCY.map(_.value)
                          ),
                          optSecurity.map(_.security),
                          optArrearsPrevYears,
                          optOutstandingArrearsOnLoan
                        )
                      }
                    } else {
                      None
                    }
                }
            }
            .toList
            .flatten
        )
      )
    } else {
      None
    }
  }

  def transformFromEtmp(
    userAnswers: UserAnswers,
    srn: Srn,
    loans: Loans
  ): Try[UserAnswers] = {

    val loanTransactions = loans.loanTransactions.toList
    val optRecordVersion = loans.recordVersion.map(LoansRecordVersionPage(srn) -> _)
    val optSchemeHadLoans = loans.optSchemeHadLoans.map(LoansMadeOrOutstandingPage(srn) -> _)

    for {
      indexes <- buildIndexesForMax5000(loanTransactions.size)
      identityTypes = indexes.map(
        index =>
          IdentityTypePage(srn, index, IdentitySubject.LoanRecipient) -> loanTransactions(index.value - 1).recipientIdentityType.identityType
      )
      loanRecipientName = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType != IdentityType.Other
          }
        )
        .map(
          index => {
            val recipientName = loanTransactions(index.value - 1).loanRecipientName
            loanTransactions(index.value - 1).recipientIdentityType.identityType match {
              case IdentityType.Individual => IndividualRecipientNamePage(srn, index) -> recipientName
              case IdentityType.UKCompany => CompanyRecipientNamePage(srn, index) -> recipientName
              case IdentityType.UKPartnership => PartnershipRecipientNamePage(srn, index) -> recipientName
            }
          }
        )
      otherRecipientName = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.Other
          }
        )
        .map(
          index => {
            val recipientName = loanTransactions(index.value - 1).loanRecipientName
            val description = loanTransactions(index.value - 1).recipientIdentityType.otherDescription
            OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient) -> RecipientDetails(
              recipientName,
              description.getOrElse("")
            )
          }
        )
      nino = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.Individual &&
              loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            IndividualRecipientNinoPage(srn, index) -> ConditionalYesNo.yes[String, Nino](
              Nino(loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse(""))
            )
          }
        )
      noNino = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.Individual &&
              loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            IndividualRecipientNinoPage(srn, index) -> ConditionalYesNo
              .no[String, Nino](loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse(""))
          }
        )
      crn = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.UKCompany &&
              loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient) -> ConditionalYesNo.yes[String, Crn](
              Crn(loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse(""))
            )
          }
        )
      noCrn = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.UKCompany &&
              loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient) -> ConditionalYesNo
              .no[String, Crn](loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse(""))
          }
        )
      utr = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.UKPartnership &&
              loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient) -> ConditionalYesNo.yes[String, Utr](
              Utr(loanTransactions(index.value - 1).recipientIdentityType.idNumber.getOrElse(""))
            )
          }
        )
      noUtr = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.UKPartnership &&
              loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse("").nonEmpty
          }
        )
        .map(
          index => {
            PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient) -> ConditionalYesNo
              .no[String, Utr](loanTransactions(index.value - 1).recipientIdentityType.reasonNoIdNumber.getOrElse(""))
          }
        )
      individualConnectedPartyStatus = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType == IdentityType.Individual
          }
        )
        .map(index => IsIndividualRecipientConnectedPartyPage(srn, index) -> true)
      sponsoringEmployer = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).recipientIdentityType.identityType != IdentityType.Individual
          }
        )
        .map(
          index =>
            RecipientSponsoringEmployerConnectedPartyPage(srn, index) -> {
              val sponsoring = loanTransactions(index.value - 1).optRecipientSponsoringEmployer
              val connected = loanTransactions(index.value - 1).connectedPartyStatus
              (sponsoring, connected) match {
                case (Some(_), _) => SponsoringOrConnectedParty.Sponsoring
                case (None, true) => SponsoringOrConnectedParty.ConnectedParty
                case (None, false) => SponsoringOrConnectedParty.Neither
              }
            }
        )
      datePeriodLoan = indexes.map(
        index =>
          DatePeriodLoanPage(srn, index) -> (loanTransactions(index.value - 1).datePeriodLoanDetails.dateOfLoan,
          Money(loanTransactions(index.value - 1).datePeriodLoanDetails.loanTotalSchemeAssets),
          loanTransactions(index.value - 1).datePeriodLoanDetails.loanPeriodInMonths)
      )
      fullLoanAmount = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).loanAmountDetails.optCapRepaymentCY.nonEmpty &&
              loanTransactions(index.value - 1).loanAmountDetails.optAmountOutstanding.nonEmpty
          }
        )
        .map(
          index =>
            AmountOfTheLoanPage(srn, index) -> {
              AmountOfTheLoan(
                Money(loanTransactions(index.value - 1).loanAmountDetails.loanAmount),
                Some(Money(loanTransactions(index.value - 1).loanAmountDetails.optCapRepaymentCY.get)),
                Some(Money(loanTransactions(index.value - 1).loanAmountDetails.optAmountOutstanding.get))
              )
            }
        )
      partialLoanAmount = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).loanAmountDetails.optCapRepaymentCY.isEmpty &&
              loanTransactions(index.value - 1).loanAmountDetails.optAmountOutstanding.isEmpty
          }
        )
        .map(
          index =>
            AmountOfTheLoanPage(srn, index) -> {
              AmountOfTheLoan(
                Money(loanTransactions(index.value - 1).loanAmountDetails.loanAmount),
                None,
                None
              )
            }
        )
      equalInstallments = indexes.map(
        index => AreRepaymentsInstalmentsPage(srn, index) -> loanTransactions(index.value - 1).equalInstallments
      )
      fullLoanInterest = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).loanInterestDetails.optIntReceivedCY.nonEmpty
          }
        )
        .map(
          index =>
            InterestOnLoanPage(srn, index) -> {
              InterestOnLoan(
                Money(loanTransactions(index.value - 1).loanInterestDetails.loanInterestAmount),
                Percentage(loanTransactions(index.value - 1).loanInterestDetails.loanInterestRate),
                Some(Money(loanTransactions(index.value - 1).loanInterestDetails.optIntReceivedCY.get))
              )
            }
        )
      partialLoanInterest = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).loanInterestDetails.optIntReceivedCY.isEmpty
          }
        )
        .map(
          index =>
            InterestOnLoanPage(srn, index) -> {
              InterestOnLoan(
                Money(loanTransactions(index.value - 1).loanInterestDetails.loanInterestAmount),
                Percentage(loanTransactions(index.value - 1).loanInterestDetails.loanInterestRate),
                None
              )
            }
        )
      securityGiven = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).optSecurityGivenDetails.nonEmpty
          }
        )
        .map(
          index =>
            SecurityGivenForLoanPage(srn, index) ->
              ConditionalYesNo
                .yes[Unit, Security](Security(loanTransactions(index.value - 1).optSecurityGivenDetails.get))
        )
      securityNotGiven = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).optSecurityGivenDetails.isEmpty
          }
        )
        .map(
          index =>
            SecurityGivenForLoanPage(srn, index) ->
              ConditionalYesNo.no[Unit, Security](())
        )
      arrearsPrevYears = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).optArrearsPrevYears.nonEmpty
          }
        )
        .map(
          index => ArrearsPrevYears(srn, index) -> loanTransactions(index.value - 1).optArrearsPrevYears.get
        )
      outstandingArrearsOnLoan = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).optOutstandingArrearsOnLoan.nonEmpty
          }
        )
        .map(
          index =>
            OutstandingArrearsOnLoanPage(srn, index) -> ConditionalYesNo
              .yes[Unit, Money](Money(loanTransactions(index.value - 1).optOutstandingArrearsOnLoan.get))
        )
      noOutstandingArrearsOnLoan = indexes
        .filter(
          index => {
            loanTransactions(index.value - 1).optOutstandingArrearsOnLoan.isEmpty
          }
        )
        .map(
          index => OutstandingArrearsOnLoanPage(srn, index) -> ConditionalYesNo.no[Unit, Money](())
        )
      loanCompleted = indexes.map(
        index => LoanCompleted(srn, index) -> SectionCompleted
      )

      loanProgress = indexes.map(
        index => LoansProgress(srn, index) -> SectionJourneyStatus.Completed
      )

      ua0 <- optRecordVersion.foldLeft(Try(userAnswers)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua1 <- optSchemeHadLoans.foldLeft(Try(ua0)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua2 <- identityTypes.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- loanRecipientName.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua31 <- otherRecipientName.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua4 <- nino.foldLeft(Try(ua31)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua41 <- noNino.foldLeft(Try(ua4)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua42 <- crn.foldLeft(Try(ua41)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua43 <- noCrn.foldLeft(Try(ua42)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua44 <- utr.foldLeft(Try(ua43)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua45 <- noUtr.foldLeft(Try(ua44)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua5 <- individualConnectedPartyStatus.foldLeft(Try(ua45)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua51 <- sponsoringEmployer.foldLeft(Try(ua5)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua6 <- datePeriodLoan.foldLeft(Try(ua51)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua7 <- fullLoanAmount.foldLeft(Try(ua6)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua8 <- partialLoanAmount.foldLeft(Try(ua7)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua9 <- equalInstallments.foldLeft(Try(ua8)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua10 <- fullLoanInterest.foldLeft(Try(ua9)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua11 <- partialLoanInterest.foldLeft(Try(ua10)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua12 <- securityGiven.foldLeft(Try(ua11)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua13 <- securityNotGiven.foldLeft(Try(ua12)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua14 <- arrearsPrevYears.foldLeft(Try(ua13)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua15 <- outstandingArrearsOnLoan.foldLeft(Try(ua14)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua16 <- noOutstandingArrearsOnLoan.foldLeft(Try(ua15)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua17 <- loanCompleted.foldLeft(Try(ua16)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua18 <- loanProgress.foldLeft(Try(ua17)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua18
  }
}
