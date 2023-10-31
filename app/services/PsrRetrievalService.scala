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
  MinimalRequiredSubmissionTransformer
}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsrRetrievalService @Inject()(
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer
) {

  def getStandardPsrDetails(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = {

    val srnValue = request.schemeDetails.srn
    val srn = Srn(srnValue).get
    val emptyUserAnswers = UserAnswers(request.getUserId + srnValue)

    for {
      psrDetails <- psrConnector.getStandardPsrDetails(
        request.schemeDetails.pstr,
        optFbNumber,
        optPeriodStartDate,
        optPsrVersion
      )

      transformedMinimal <- {
        if (psrDetails.isEmpty) {
          Future(emptyUserAnswers)
        } else {
          Future.fromTry(
            minimalRequiredSubmissionTransformer
              .transformFromEtmp(
                emptyUserAnswers,
                srn,
                request.pensionSchemeId,
                psrDetails.get.minimalRequiredSubmission
              )
          )
        }
      }

      transformedLoans <- {
        if (psrDetails.isEmpty || psrDetails.get.loans.isEmpty) {
          Future(transformedMinimal)
        } else {
          Future.fromTry(
            loanTransactionsTransformer
              .transformFromEtmp(
                transformedMinimal,
                srn,
                psrDetails.get.loans.get.loanTransactions.toList
              )
          )
        }
      }

      transformedAssets <- {
        if (psrDetails.isEmpty || psrDetails.get.assets.isEmpty) {
          Future(transformedLoans)
        } else {
          Future.fromTry(
            landOrPropertyTransactionsTransformer
              .transformFromEtmp(
                transformedLoans,
                srn,
                psrDetails.get.assets.get.landOrProperty
              )
          )
        }
      }
    } yield {
      transformedAssets
    }
  }
}
