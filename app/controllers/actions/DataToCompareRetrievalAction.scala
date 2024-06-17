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
import com.google.inject.ImplementedBy
import config.Constants.UNCHANGED_SESSION_PREFIX
import models.UserAnswers
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, DataRequest, OptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class DataToCompareRetrievalAction(
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService,
  year: String,
  current: Int,
  previous: Int
)(implicit override val executionContext: ExecutionContext)
    extends ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {

  override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val emptyUserAnswers = UserAnswers(request.getUserId + request.srn) // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
    val dataRequest: DataRequest[A] = DataRequest[A](request, emptyUserAnswers)
    val userAnswersKey = request.getUserId + request.srn
    for {
      pureUa <- sessionRepository.get(UNCHANGED_SESSION_PREFIX + userAnswersKey)
      currentReturn <- psrRetrievalService.getStandardPsrDetails(
        None,
        Some(year),
        Some("%03d".format(current)),
        controllers.routes.OverviewController.onPageLoad(request.srn)
      )(hc = implicitly, ec = implicitly, request = dataRequest)
      previousReturn <- psrRetrievalService.getStandardPsrDetails(
        None,
        Some(year),
        Some("%03d".format(previous)),
        controllers.routes.OverviewController.onPageLoad(request.srn)
      )(hc = implicitly, ec = implicitly, request = dataRequest)
    } yield OptionalDataRequest(
      request,
      Some(currentReturn),
      pureUa,
      if (previousReturn.data == emptyUserAnswers.data) None else Some(previousReturn),
      Some(year),
      Some(current),
      Some(previous)
    )
  }

}

@ImplementedBy(classOf[DataToCompareRetrievalActionProviderImpl])
trait DataToCompareRetrievalActionProvider {
  def apply(year: String, current: Int, previous: Int): ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
}

class DataToCompareRetrievalActionProviderImpl @Inject()(
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService
)(implicit val ec: ExecutionContext)
    extends DataToCompareRetrievalActionProvider {

  def apply(year: String, current: Int, previous: Int): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new DataToCompareRetrievalAction(sessionRepository, psrRetrievalService, year, current, previous)
}
