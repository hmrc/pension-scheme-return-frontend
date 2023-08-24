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
import models._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRSubmissionService @Inject()(psrConnector: PSRConnector) {

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
  ): Future[Unit] = {
    val submission = MinimalRequiredSubmission(
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
    psrConnector.submitMinimalRequiredDetails(submission)
  }

}
