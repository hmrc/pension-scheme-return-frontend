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
import models.UserAnswers
import models.requests.DataRequest
import models.requests.psr._
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.landorproperty.LandOrPropertyHeldPage
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import pages.nonsipp.shares.DidSchemeHoldAnySharesPage
import transformations._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsrSubmissionService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer,
  memberPaymentsTransformer: MemberPaymentsTransformer,
  moneyBorrowedTransformer: MoneyBorrowedTransformer,
  sharesTransformer: SharesTransformer
) {

  def submitPsrDetails(
    srn: Srn,
    userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    submitPsrDetails(srn)(implicitly, implicitly, DataRequest(request.request, userAnswers))

  def submitPsrDetails(
    srn: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] = {

    val optSchemeHadLoans = request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val optLandOrPropertyHeld = request.userAnswers.get(LandOrPropertyHeldPage(srn))
    val optMoneyWasBorrowed = request.userAnswers.get(MoneyBorrowedPage(srn))
    val optDisposeAnyLandOrProperty = request.userAnswers.get(LandOrPropertyDisposalPage(srn))
    val optDidSchemeHoldAnyShares = request.userAnswers.get(DidSchemeHoldAnySharesPage(srn))

    (
      minimalRequiredSubmissionTransformer.transformToEtmp(srn),
      request.userAnswers.get(CheckReturnDatesPage(srn))
    ).mapN { (minimalRequiredSubmission, checkReturnDates) =>
      psrConnector.submitPsrDetails(
        PsrSubmission(
          minimalRequiredSubmission = minimalRequiredSubmission,
          checkReturnDates = checkReturnDates,
          loans = buildLoans(srn)(optSchemeHadLoans),
          assets = buildAssets(srn)(optLandOrPropertyHeld, optMoneyWasBorrowed, optDisposeAnyLandOrProperty),
          membersPayments = memberPaymentsTransformer.transformToEtmp(srn, request.userAnswers),
          shares = buildShares(srn)(optDidSchemeHoldAnyShares)
        )
      )
    }.sequence
  }

  private def buildLoans(
    srn: Srn
  )(optSchemeHadLoans: Option[Boolean])(implicit request: DataRequest[_]): Option[Loans] =
    optSchemeHadLoans.map(
      schemeHadLoans => Loans(schemeHadLoans, loanTransactionsTransformer.transformToEtmp(srn))
    )

  private def buildAssets(srn: Srn)(
    optLandOrPropertyHeld: Option[Boolean],
    optMoneyWasBorrowed: Option[Boolean],
    optDisposeAnyLandOrProperty: Option[Boolean]
  )(implicit request: DataRequest[_]): Option[Assets] =
    Option.when(List(optLandOrPropertyHeld, optMoneyWasBorrowed, optDisposeAnyLandOrProperty).flatten.nonEmpty)(
      Assets(
        optLandOrProperty = optLandOrPropertyHeld.map(landOrPropertyHeld => {
          val disposeAnyLandOrProperty = optDisposeAnyLandOrProperty.getOrElse(false)
          LandOrProperty(
            landOrPropertyHeld = landOrPropertyHeld,
            disposeAnyLandOrProperty = disposeAnyLandOrProperty,
            landOrPropertyTransactions =
              landOrPropertyTransactionsTransformer.transformToEtmp(srn, disposeAnyLandOrProperty)
          )
        }),
        optBorrowing = optMoneyWasBorrowed.map(
          moneyWasBorrowed =>
            Borrowing(
              moneyWasBorrowed = moneyWasBorrowed,
              moneyBorrowed = moneyBorrowedTransformer.transformToEtmp(srn)
            )
        )
      )
    )

  private def buildShares(srn: Srn)(
    optDidSchemeHoldAnyShares: Option[Boolean]
  )(implicit request: DataRequest[_]): Option[Shares] =
    optDidSchemeHoldAnyShares.map(
      _ =>
        Shares(
          optShareTransactions = sharesTransformer.transformToEtmp(srn)
        )
    )
}
