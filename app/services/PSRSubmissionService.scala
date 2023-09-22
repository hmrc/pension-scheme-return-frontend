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

package services

import cats.data.NonEmptyList
import cats.implicits._
import config.Refined.Max5000
import connectors.PSRConnector
import eu.timepit.refined.refineV
import models.ConditionalYesNo._
import models.IdentitySubject.LoanRecipient
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{RecipientIdentityType, _}
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypes, OtherRecipientDetailsPage, PartnershipRecipientUtrPage}
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, WhyNoBankAccountPage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRSubmissionService @Inject()(psrConnector: PSRConnector, schemeDateService: SchemeDateService) {

  private type OptionalRecipientDetails = Option[(String, RecipientIdentityType, Option[Boolean], Option[String])]

  def submitMinimalRequiredDetails(
    pstr: String,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    accountingPeriods: NonEmptyList[DateRange],
    reasonForNoBankAccount: Option[String],
    schemeMemberNumbers: SchemeMemberNumbers
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] =
    psrConnector.submitMinimalRequiredDetails(
      buildMinimalRequiredDetails(
        pstr,
        periodStart,
        periodEnd,
        accountingPeriods,
        reasonForNoBankAccount,
        schemeMemberNumbers
      )
    )

  def submitLoanDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    (
      schemeDateService.returnPeriods(srn),
      request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId)),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    ).mapN { (returnPeriods, schemeMemberNumbers, checkReturnDates, schemeHadLoans) =>
      val reasonForNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))

      val loanTransactions = request.userAnswers
        .map(IdentityTypes(srn, LoanRecipient))
        .map {
          case (key, identityType) =>
            key.toIntOption.flatMap(i => refineV[Max5000.Refined](i).toOption) match {
              case None => None
              case Some(index) =>
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
                          Some(connectedParty),
                          None
                        )
                      case (name, Right(nino), connectedParty) =>
                        (
                          name,
                          RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
                          Some(connectedParty),
                          None
                        )
                    }
                  case IdentityType.UKCompany =>
                    (
                      request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
                      request.userAnswers
                        .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
                        .map(_.value),
                      request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index)).map(_.name)
                    ).mapN {
                      case (name, Left(noCrnReason), recipientSponsoringEmployer) =>
                        (
                          name,
                          RecipientIdentityType(IdentityType.UKCompany, None, Some(noCrnReason), None),
                          None,
                          Some(recipientSponsoringEmployer)
                        )
                      case (name, Right(crn), recipientSponsoringEmployer) =>
                        (
                          name,
                          RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
                          None,
                          Some(recipientSponsoringEmployer)
                        )
                    }
                  case IdentityType.UKPartnership =>
                    (
                      request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
                      request.userAnswers
                        .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
                        .map(_.value),
                      request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index)).map(_.name)
                    ).mapN {
                      case (name, Left(noUtrReason), recipientSponsoringEmployer) =>
                        (
                          name,
                          RecipientIdentityType(IdentityType.UKPartnership, None, Some(noUtrReason), None),
                          None,
                          Some(recipientSponsoringEmployer)
                        )
                      case (name, Right(utr), recipientSponsoringEmployer) =>
                        (
                          name,
                          RecipientIdentityType(IdentityType.UKPartnership, Some(utr.value), None, None),
                          None,
                          Some(recipientSponsoringEmployer)
                        )
                    }
                  case IdentityType.Other =>
                    (
                      request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)),
                      request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index)).map(_.name)
                    ).mapN(
                      (other, recipientSponsoringEmployer) =>
                        (
                          other.name,
                          RecipientIdentityType(IdentityType.Other, None, None, Some(other.description)),
                          None,
                          Some(recipientSponsoringEmployer)
                        )
                    )
                }

                for {
                  recipientIdentityDetails <- optRecipientIdentityDetails
                  (recipientName, recipientIdentityType, optConnectedParty, optRecipientSponsoringEmployer) = recipientIdentityDetails
                  equalInstallments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index))
                  datePeriodLoanDetails <- request.userAnswers.get(DatePeriodLoanPage(srn, index))
                  loanAmountDetails <- request.userAnswers.get(AmountOfTheLoanPage(srn, index))
                  optSecurity <- request.userAnswers.get(SecurityGivenForLoanPage(srn, index)).map(_.value.toOption)
                  loanInterestDetails <- request.userAnswers.get(InterestOnLoanPage(srn, index))
                  optOutstandingArrearsOnLoan <- request.userAnswers
                    .get(OutstandingArrearsOnLoanPage(srn, index))
                    .map(_.value.toOption)
                } yield {
                  LoanTransactions(
                    recipientIdentityType,
                    recipientName,
                    optConnectedParty,
                    optRecipientSponsoringEmployer,
                    LoanPeriod(datePeriodLoanDetails._1, datePeriodLoanDetails._2.value, datePeriodLoanDetails._3),
                    LoanAmountDetails(
                      loanAmountDetails._1.value,
                      loanAmountDetails._2.value,
                      loanAmountDetails._3.value
                    ),
                    equalInstallments,
                    LoanInterestDetails(
                      loanInterestDetails._1.value,
                      loanInterestDetails._2.value,
                      loanInterestDetails._3.value
                    ),
                    optSecurity.map(_.security),
                    optOutstandingArrearsOnLoan.map(_.value)
                  )
                }
            }
        }
        .toList
        .flatten

      psrConnector.submitLoansDetails(
        buildLoanDetails(
          request.schemeDetails.pstr,
          returnPeriods.last.to,
          returnPeriods.last.from,
          returnPeriods,
          reasonForNoBankAccount,
          schemeMemberNumbers,
          checkReturnDates,
          schemeHadLoans,
          loanTransactions
        )
      )
    }.sequence

  private def buildLoanDetails(
    pstr: String,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    accountingPeriods: NonEmptyList[DateRange],
    reasonForNoBankAccount: Option[String],
    schemeMemberNumbers: SchemeMemberNumbers,
    checkReturnDates: Boolean,
    schemeHadLoans: Boolean,
    loanTransactions: Seq[LoanTransactions]
  ): LoansSubmission = {
    val minimalRequiredDetails = buildMinimalRequiredDetails(
      pstr,
      periodStart,
      periodEnd,
      accountingPeriods,
      reasonForNoBankAccount,
      schemeMemberNumbers
    )
    LoansSubmission(minimalRequiredDetails, checkReturnDates, Loans(schemeHadLoans, loanTransactions))
  }

  private def buildMinimalRequiredDetails(
    pstr: String,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    accountingPeriods: NonEmptyList[DateRange],
    reasonForNoBankAccount: Option[String],
    schemeMemberNumbers: SchemeMemberNumbers
  ) =
    MinimalRequiredSubmission(
      ReportDetails(pstr, periodStart, periodEnd),
      accountingPeriods.map(range => range.from -> range.to),
      SchemeDesignatory(
        openBankAccount = reasonForNoBankAccount.isEmpty,
        reasonForNoBankAccount,
        schemeMemberNumbers.noOfActiveMembers,
        schemeMemberNumbers.noOfDeferredMembers,
        schemeMemberNumbers.noOfPensionerMembers,
        schemeMemberNumbers.total
      )
    )

}
