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

import connectors.PSRConnector
import models.SchemeId.Srn
import models.UserAnswers
import models.requests.DataRequest
import play.api.mvc.AnyContent
import transformations.{
  LandOrPropertyTransactionsTransformer,
  LoanTransactionsTransformer,
  MinimalRequiredSubmissionTransformer,
  MoneyBorrowedTransformer
}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PsrRetrievalService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer,
  moneyBorrowedTransformer: MoneyBorrowedTransformer
) {

  def getStandardPsrDetails(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UserAnswers] = {

    val srnValue = request.schemeDetails.srn
    val srn = Srn(srnValue).get
    val emptyUserAnswers = UserAnswers(request.getUserId + srnValue)

    psrConnector
      .getStandardPsrDetails(
        request.schemeDetails.pstr,
        optFbNumber,
        optPeriodStartDate,
        optPsrVersion
      )
      .flatMap {
        case Some(psrDetails) =>
          val result = for {
            transformedMinimalUa <- minimalRequiredSubmissionTransformer
              .transformFromEtmp(
                emptyUserAnswers,
                srn,
                request.pensionSchemeId,
                psrDetails.minimalRequiredSubmission
              )

            transformedLoansUa <- psrDetails.loans
              .map(
                l =>
                  loanTransactionsTransformer
                    .transformFromEtmp(
                      transformedMinimalUa,
                      srn,
                      l.loanTransactions.toList
                    )
              )
              .getOrElse(Try(transformedMinimalUa))

            transformedLandOrPropertyAssetsUa <- psrDetails.assets
              .map(
                a =>
                  if (a.landOrProperty.landOrPropertyHeld) {
                    landOrPropertyTransactionsTransformer
                      .transformFromEtmp(
                        transformedLoansUa,
                        srn,
                        a.landOrProperty
                      )
                  } else {
                    Try(transformedLoansUa)
                  }
              )
              .getOrElse(Try(transformedLoansUa))

            transformedMoneyBorrowingAssets <- psrDetails.assets
              .map(
                a =>
                  if (a.borrowing.moneyWasBorrowed) {
                    moneyBorrowedTransformer
                      .transformFromEtmp(
                        transformedLandOrPropertyAssetsUa,
                        srn,
                        a.borrowing
                      )
                  } else {
                    Try(transformedLandOrPropertyAssetsUa)
                  }
              )
              .getOrElse(Try(transformedLandOrPropertyAssetsUa))
          } yield {
            transformedMoneyBorrowingAssets
          }
          Future.fromTry(result)
        case _ => Future(emptyUserAnswers)
      }
  }
}
