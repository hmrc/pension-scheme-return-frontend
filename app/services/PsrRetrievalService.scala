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
import pages.nonsipp.loansmadeoroutstanding._
import play.api.mvc.AnyContent
import repositories.SessionRepository
import transformations.{
  LandOrPropertyTransactionsTransformer,
  LoanTransactionsTransformer,
  MinimalRequiredSubmissionTransformer
}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsrRetrievalService @Inject()(
  sessionRepository: SessionRepository,
  psrConnector: PSRConnector,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer
) {

  def getStandardPsrDetails(
    request: DataRequest[AnyContent],
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] =
    for {
      psrDetails <- psrConnector.getStandardPsrDetails(
        request.schemeDetails.pstr,
        optFbNumber,
        optPeriodStartDate,
        optPsrVersion
      )
      userAnswersKey = request.getUserId + request.schemeDetails.srn
      userAnswers <- Future(Some(UserAnswers(userAnswersKey)))
      transformedFromEtmp <- {
        if (psrDetails.isEmpty) {
          Future(userAnswers.get)
        } else {
          Future.fromTry(
            minimalRequiredSubmissionTransformer
              .transformFromEtmp(
                userAnswers.get,
                Srn(request.schemeDetails.srn).get,
                request.pensionSchemeId,
                psrDetails.get.minimalRequiredSubmission
              )
          )
        }
      }
    } yield {
      transformedFromEtmp
    }
}
