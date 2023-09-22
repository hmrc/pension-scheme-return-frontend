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
import connectors.PSRConnector
import models.SchemeId.Srn
import models.{RecipientIdentityType, _}
import models.requests.DataRequest
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientNamePage, IndividualRecipientNamePage, IndividualRecipientNinoPage, LoansMadeOrOutstandingPage, PartnershipRecipientNamePage}
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, WhyNoBankAccountPage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import config.Refined.Max5000
import eu.timepit.refined.refineV
import models.IdentitySubject.LoanRecipient
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, IdentityTypes, OtherRecipientDetailsPage, PartnershipRecipientUtrPage}

class PSRSubmissionService @Inject()(psrConnector: PSRConnector, schemeDateService: SchemeDateService) {

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

      request.userAnswers.map(IdentityTypes(srn, LoanRecipient)).map {
        case (key, identityType) =>
          key.toIntOption.flatMap(i => refineV[Max5000.Refined](i).toOption) match {
            case None => None
            case Some(index) =>

              val recipientIdentity: Option[(String, RecipientIdentityType)] = identityType match {
                case IdentityType.Individual =>
                  (
                    request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
                    request.userAnswers.get(IndividualRecipientNinoPage(srn, index)).map(_.value)
                  ).mapN {
                    case (name, Left(noNinoReason)) =>
                      (name, RecipientIdentityType(IdentityType.Individual, None, Some(noNinoReason), None))
                    case (name, Right(nino)) =>
                      (name, RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None))
                  }
                case IdentityType.UKCompany =>
                  (
                    request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
                    request.userAnswers.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient)).map(_.value)
                  ).mapN{
                    case (name, Left(noCrnReason)) =>
                      (name, RecipientIdentityType(IdentityType.UKCompany, None, Some(noCrnReason), None))
                    case (name, Right(crn)) =>
                      (name, RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None))
                  }
                case IdentityType.UKPartnership =>
                  (
                    request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
                    request.userAnswers.get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient)).map(_.value)
                  ).mapN{
                    case (name, Left(noUtrReason)) =>
                      (name, RecipientIdentityType(IdentityType.UKPartnership, None, Some(noUtrReason), None))
                    case (name, Right(utr)) =>
                      (name, RecipientIdentityType(IdentityType.UKPartnership, Some(utr.value), None, None))
                  }
                case IdentityType.Other =>
                  request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)).map(
                    other => (other.name, RecipientIdentityType(IdentityType.Other, None, None, Some(other.description)))
                  )
              }

              for {
                r <- recipientIdentity
                (recipientName, recipientIdentity) = r
              }

              LoanTransactions(
                recipientIdentity,
                recipientName,
                ???,
                ???,
                ???,
                ???,
                ???,
                ???,
                ???
              )
          }
      }

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
          ???
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
