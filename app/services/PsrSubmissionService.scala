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
import pages.nonsipp.landorproperty.LandOrPropertyHeldPage
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import transformations.{
  LandOrPropertyTransactionsTransformer,
  LoanTransactionsTransformer,
  MinimalRequiredSubmissionTransformer,
  MoneyBorrowedTransformer
}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsrSubmissionService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer,
  moneyBorrowedTransformer: MoneyBorrowedTransformer
) {

  def submitPsrDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] = {

    val schemeHadLoans = request.userAnswers.get(LoansMadeOrOutstandingPage(srn)).getOrElse(false)
    val landOrPropertyHeld = request.userAnswers.get(LandOrPropertyHeldPage(srn)).getOrElse(false)
    val moneyWasBorrowed: Boolean = request.userAnswers.get(MoneyBorrowedPage(srn)).getOrElse(false)
    (
      minimalRequiredSubmissionTransformer.transformToEtmp(srn),
      request.userAnswers.get(CheckReturnDatesPage(srn))
    ).mapN { (minimalRequiredSubmission, checkReturnDates) =>
      psrConnector.submitPsrDetails(
        PsrSubmission(
          minimalRequiredSubmission = minimalRequiredSubmission,
          checkReturnDates = checkReturnDates,
          loans = Option.when(schemeHadLoans)(Loans(schemeHadLoans, loanTransactionsTransformer.transformToEtmp(srn))),
          assets = Option.when(landOrPropertyHeld || moneyWasBorrowed)(
            Assets(
              landOrProperty = LandOrProperty(
                landOrPropertyHeld = landOrPropertyHeld,
                landOrPropertyTransactions = landOrPropertyTransactionsTransformer.transformToEtmp(srn)
              ),
              borrowing = Borrowing(
                moneyWasBorrowed = moneyWasBorrowed,
                moneyBorrowed = moneyBorrowedTransformer.transformToEtmp(srn)
              )
            )
          )
        )
      )
    }.sequence
  }
}
