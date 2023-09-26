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

import cats.implicits._
import config.Refined.OneTo5000
import connectors.PSRConnector
import eu.timepit.refined.refineV
import models.ConditionalYesNo._
import models.SchemeId.Srn
import models._
import models.requests.psr._
import models.requests.{psr, DataRequest}
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypes,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, WhyNoBankAccountPage}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRSubmissionService @Inject()(psrConnector: PSRConnector, schemeDateService: SchemeDateService) {

  private type OptionalRecipientDetails = Option[(String, RecipientIdentityType, Option[Boolean], Option[String])]

  def submitMinimalRequiredDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    buildMinimalRequiredDetails(srn).map(psrConnector.submitMinimalRequiredDetails(_)).sequence

  def submitPsrDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    (
      buildMinimalRequiredDetails(srn),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    ).mapN { (minimalRequiredDetails, checkReturnDates, schemeHadLoans) =>
      psrConnector.submitPsrSubmissionDetails(
        PsrSubmission(
          minimalRequiredSubmission = minimalRequiredDetails,
          checkReturnDates = checkReturnDates,
          loans = if (schemeHadLoans) Some(Loans(schemeHadLoans, buildLoanTransactions(srn))) else None
        )
      )
    }.sequence

  private def buildLoanTransactions(srn: Srn)(implicit request: DataRequest[_]): List[LoanTransactions] = {
    request.userAnswers
      .map(IdentityTypes(srn, IdentitySubject.LoanRecipient))
      .map {
        case (key, identityType) =>
          key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
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
                (loanAmount, capRepaymentCY, amountOutstanding) = loanAmountDetails
                optSecurity <- request.userAnswers.get(SecurityGivenForLoanPage(srn, index)).map(_.value.toOption)
                loanInterestDetails <- request.userAnswers.get(InterestOnLoanPage(srn, index))
                (loanInterestAmount, loanInterestRate, intReceivedCY) = loanInterestDetails
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
                    loanAmount.value,
                    capRepaymentCY.value,
                    amountOutstanding.value
                  ),
                  equalInstallments,
                  LoanInterestDetails(
                    loanInterestAmount.value,
                    loanInterestRate.value,
                    intReceivedCY.value
                  ),
                  optSecurity.map(_.security),
                  optOutstandingArrearsOnLoan.map(_.value)
                )
              }
          }
      }
      .toList
      .flatten
  }

  private def buildMinimalRequiredDetails(srn: Srn)(implicit request: DataRequest[_]) = {
    val reasonForNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
    (
      schemeDateService.returnPeriods(srn),
      request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
    ).mapN { (returnPeriods, schemeMemberNumbers) =>
      MinimalRequiredSubmission(
        ReportDetails(request.schemeDetails.pstr, returnPeriods.last.to, returnPeriods.last.from),
        returnPeriods.map(range => range.from -> range.to),
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
  }

}
