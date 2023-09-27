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
import connectors.PSRConnector
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr._
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.loansmadeoroutstanding._
import transformations.{LoanTransactionsTransformer, MinimalRequiredSubmissionTransformer}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRSubmissionService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer
) {

  def submitMinimalRequiredDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    minimalRequiredSubmissionTransformer.transform(srn).map(psrConnector.submitMinimalRequiredDetails(_)).sequence

  def submitPsrDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    (
      minimalRequiredSubmissionTransformer.transform(srn),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    ).mapN { (minimalRequiredSubmission, checkReturnDates, schemeHadLoans) =>
      psrConnector.submitPsrDetails(
        PsrSubmission(
          minimalRequiredSubmission = minimalRequiredSubmission,
          checkReturnDates = checkReturnDates,
          loans = if (schemeHadLoans) Some(Loans(schemeHadLoans, loanTransactionsTransformer.transform(srn))) else None
        )
      )
    }.sequence

}
