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

import services.{PrePopulationService, PsrRetrievalService}
import play.api.mvc.ActionTransformer
import com.google.inject.ImplementedBy
import models.SchemeId.Srn
import config.Constants.UNCHANGED_SESSION_PREFIX
import models.UserAnswers
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, DataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PrePopulationDataAction @Inject()(
  optLastSubmittedPsrFbInPreviousYears: Option[String],
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService,
  prePopulationService: PrePopulationService
)(
  implicit val ec: ExecutionContext
) extends ActionTransformer[DataRequest, DataRequest] {

  override def transform[A](existingDataRequest: DataRequest[A]): Future[DataRequest[A]] =
    optLastSubmittedPsrFbInPreviousYears
      .map { lastSubmittedPsrFbInPreviousYears =>
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromRequestAndSession(existingDataRequest, existingDataRequest.session)
        val allowedAccessRequest = existingDataRequest.request
        val srn: Srn = existingDataRequest.srn
        for {
          baseReturnUA <- psrRetrievalService.getAndTransformStandardPsrDetails(
            optFbNumber = Some(lastSubmittedPsrFbInPreviousYears),
            fallBackCall = controllers.routes.OverviewController.onPageLoad(srn)
          )(
            hc = implicitly,
            ec = implicitly,
            request = DataRequest[A](allowedAccessRequest, emptyUserAnswers(allowedAccessRequest))
          )
          prePoppedUA <- Future.fromTry(
            prePopulationService.buildPrePopulatedUserAnswers(baseReturnUA, existingDataRequest.userAnswers)(srn)
          )
          _ <- sessionRepository.set(prePoppedUA.copy(id = UNCHANGED_SESSION_PREFIX + prePoppedUA.id))
        } yield {
          DataRequest(allowedAccessRequest, prePoppedUA)
        }

      }
      .getOrElse(Future.successful(existingDataRequest))

  override protected def executionContext: ExecutionContext = ec

  // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
  private def emptyUserAnswers(request: AllowedAccessRequest[_]): UserAnswers =
    UserAnswers(request.getUserId + request.srn)
}

@ImplementedBy(classOf[PrePopulationDataActionProviderImpl])
trait PrePopulationDataActionProvider {
  def apply(optLastSubmittedPsrFbInPreviousYears: Option[String]): ActionTransformer[DataRequest, DataRequest]
}

class PrePopulationDataActionProviderImpl @Inject()(
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService,
  prePopulationService: PrePopulationService
)(implicit val ec: ExecutionContext)
    extends PrePopulationDataActionProvider {

  def apply(optLastSubmittedPsrFbInPreviousYears: Option[String]): PrePopulationDataAction =
    new PrePopulationDataAction(
      optLastSubmittedPsrFbInPreviousYears,
      sessionRepository,
      psrRetrievalService,
      prePopulationService
    )
}
