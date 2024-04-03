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

import play.api.mvc.AnyContent
import connectors.PSRConnector
import transformations._
import uk.gov.hmrc.http.HeaderCarrier
import models.UserAnswers
import models.requests.DataRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.Inject

class PsrRetrievalService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer,
  moneyBorrowedTransformer: MoneyBorrowedTransformer,
  memberPaymentsTransformer: MemberPaymentsTransformer,
  sharesTransformer: SharesTransformer,
  bondTransactionsTransformer: BondTransactionsTransformer,
  otherAssetTransactionsTransformer: OtherAssetTransactionsTransformer
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

    val srn = request.srn

    val emptyUserAnswers = UserAnswers(request.getUserId + srn)

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
                loans =>
                  loanTransactionsTransformer
                    .transformFromEtmp(
                      transformedMinimalUa,
                      srn,
                      loans
                    )
              )
              .getOrElse(Try(transformedMinimalUa))

            transformedLandOrPropertyAssetsUa <- psrDetails.assets
              .map { assets =>
                assets.optLandOrProperty
                  .map(
                    landOrProperty =>
                      landOrPropertyTransactionsTransformer
                        .transformFromEtmp(
                          transformedLoansUa,
                          srn,
                          landOrProperty
                        )
                  )
                  .getOrElse(Try(transformedLoansUa))
              }
              .getOrElse(Try(transformedLoansUa))

            transformedMoneyBorrowingAssetsUa <- psrDetails.assets
              .map { asset =>
                asset.optBorrowing
                  .map(
                    borrowing =>
                      moneyBorrowedTransformer
                        .transformFromEtmp(
                          transformedLandOrPropertyAssetsUa,
                          srn,
                          borrowing
                        )
                  )
                  .getOrElse(Try(transformedLandOrPropertyAssetsUa))
              }
              .getOrElse(Try(transformedLandOrPropertyAssetsUa))

            transformedBondsAssetsUa <- psrDetails.assets
              .map { asset =>
                asset.optBonds
                  .map(
                    bonds =>
                      bondTransactionsTransformer
                        .transformFromEtmp(
                          transformedMoneyBorrowingAssetsUa,
                          srn,
                          bonds
                        )
                  )
                  .getOrElse(Try(transformedMoneyBorrowingAssetsUa))
              }
              .getOrElse(Try(transformedMoneyBorrowingAssetsUa))

            transformedOtherAssetsUa <- psrDetails.assets
              .map { asset =>
                asset.optOtherAssets
                  .map(
                    otherAssets =>
                      otherAssetTransactionsTransformer
                        .transformFromEtmp(
                          transformedBondsAssetsUa,
                          srn,
                          otherAssets
                        )
                  )
                  .getOrElse(Try(transformedBondsAssetsUa))
              }
              .getOrElse(Try(transformedBondsAssetsUa))

            transformedMemberDetailsUa <- psrDetails.membersPayments
              .map(
                memberPayments =>
                  memberPaymentsTransformer.transformFromEtmp(transformedOtherAssetsUa, srn, memberPayments)
              )
              .getOrElse(Try(transformedOtherAssetsUa))

            transformedSharesUa <- psrDetails.shares
              .map(sh => sharesTransformer.transformFromEtmp(transformedMemberDetailsUa, srn, sh))
              .getOrElse(Try(transformedMemberDetailsUa))

          } yield {
            transformedSharesUa
          }
          Future.fromTry(result)
        case _ => Future(emptyUserAnswers)
      }
  }
}
