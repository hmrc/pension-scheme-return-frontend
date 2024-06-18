/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.Call
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
  loansTransformer: LoansTransformer,
  memberPaymentsTransformer: MemberPaymentsTransformer,
  sharesTransformer: SharesTransformer,
  assetsTransformer: AssetsTransformer,
  declarationTransformer: DeclarationTransformer
) {

  // todo should return Option.None on 404 instead of empty user answers
  def getStandardPsrDetails(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    fallBackCall: Call,
    fetchingPreviousVersion: Boolean = false
  )(
    implicit request: DataRequest[_],
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
        optPsrVersion,
        fallBackCall
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
                  loansTransformer
                    .transformFromEtmp(
                      transformedMinimalUa,
                      srn,
                      loans
                    )
              )
              .getOrElse(Try(transformedMinimalUa))

            transformedAssetsUa <- psrDetails.assets
              .map { assets =>
                assetsTransformer.transformFromEtmp(transformedLoansUa, srn, assets)
              }
              .getOrElse(Try(transformedLoansUa))

            transformedMemberDetailsUa <- psrDetails.membersPayments
              .map(
                memberPayments =>
                  memberPaymentsTransformer
                    .transformFromEtmp(
                      transformedAssetsUa,
                      request.previousUserAnswers,
                      srn,
                      memberPayments,
                      fetchingPreviousVersion
                    )
              )
              .getOrElse(Try(transformedAssetsUa))

            transformedSharesUa <- psrDetails.shares
              .map(sh => sharesTransformer.transformFromEtmp(transformedMemberDetailsUa, srn, sh))
              .getOrElse(Try(transformedMemberDetailsUa))

            transformedPsrDeclarationUa <- psrDetails.psrDeclaration
              .map(pd => declarationTransformer.transformFromEtmp(transformedSharesUa, srn, pd))
              .getOrElse(Try(transformedSharesUa))

          } yield {
            transformedPsrDeclarationUa
          }
          Future.fromTry(result)
        case _ => Future(emptyUserAnswers)
      }
  }
}
