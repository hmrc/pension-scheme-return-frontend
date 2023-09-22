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
import models._
import models.requests.DataRequest
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, WhyNoBankAccountPage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

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
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[Any]): Future[Option[Unit]] =
    (
      schemeDateService.returnPeriods(srn),
      request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId)),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    ).mapN { (returnPeriods, schemeMemberNumbers, checkReturnDates, schemeHadLoans) =>
      val reasonForNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
      psrConnector.submitLoansDetails(
        buildLoanDetails(
          request.schemeDetails.pstr,
          returnPeriods.last.to,
          returnPeriods.last.from,
          returnPeriods,
          reasonForNoBankAccount,
          schemeMemberNumbers,
          checkReturnDates,
          schemeHadLoans
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
    schemeHadLoans: Boolean
  ): LoansSubmission = {
    val minimalRequiredDetails = buildMinimalRequiredDetails(
      pstr,
      periodStart,
      periodEnd,
      accountingPeriods,
      reasonForNoBankAccount,
      schemeMemberNumbers
    )
    LoansSubmission(minimalRequiredDetails, checkReturnDates, schemeHadLoans, List(LoanTransactions()))
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
