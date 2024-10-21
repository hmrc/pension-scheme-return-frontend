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

package controllers.actions

import services.PsrRetrievalService
import play.api.mvc.ActionTransformer
import config.Constants.PREPOPULATION_PREFIX
import models.UserAnswers
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, DataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PrePopulationDataAction @Inject()(sessionRepository: SessionRepository, psrRetrievalService: PsrRetrievalService)(
  implicit val ec: ExecutionContext
) {

  def apply(optLastSubmittedPsrFbInPreviousYears: Option[String]): ActionTransformer[DataRequest, DataRequest] =
    new ActionTransformer[DataRequest, DataRequest] {

      override protected def transform[A](existingDataRequest: DataRequest[A]): Future[DataRequest[A]] =
        optLastSubmittedPsrFbInPreviousYears
          .map { lastSubmittedPsrFbInPreviousYears =>
            implicit val hc: HeaderCarrier =
              HeaderCarrierConverter.fromRequestAndSession(existingDataRequest, existingDataRequest.session)
            val allowedAccessRequest = existingDataRequest.request
            for {
              prePopulationReturn <- psrRetrievalService.getAndTransformStandardPsrDetails(
                optFbNumber = Some(lastSubmittedPsrFbInPreviousYears),
                fallBackCall = controllers.routes.OverviewController.onPageLoad(existingDataRequest.srn)
              )(
                hc = implicitly,
                ec = implicitly,
                request = DataRequest[A](allowedAccessRequest, emptyUserAnswers(allowedAccessRequest))
              )
              cleanedPrePopulationUa = cleanUpPrePopulationUA(prePopulationReturn)
              _ <- sessionRepository.set(
                cleanedPrePopulationUa.copy(id = PREPOPULATION_PREFIX + prePopulationReturn.id)
              )
            } yield {
              DataRequest(
                allowedAccessRequest,
                existingDataRequest.userAnswers,
                prePopulationUserAnswers = Some(cleanedPrePopulationUa)
              )
            }

          }
          .getOrElse(Future.successful(existingDataRequest))
      override protected def executionContext: ExecutionContext = ec

    }

  // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
  private def emptyUserAnswers(request: AllowedAccessRequest[_]): UserAnswers =
    UserAnswers(request.getUserId + request.srn)

  private def cleanUpPrePopulationUA(userAnswers: UserAnswers): UserAnswers = userAnswers
}
