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
import play.api.Logger
import models.UserAnswers
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, DataRequest, OptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class DataRetrievalETMPAction(
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService
)(implicit val ec: ExecutionContext) {

  private val logger = Logger(getClass)

  def versionForYear(year: String, current: Int): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        val emptyUserAnswers = UserAnswers(request.getUserId + request.srn) // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
        val dataRequest: DataRequest[A] = DataRequest[A](request, emptyUserAnswers)
        val userAnswersKey = request.getUserId + request.srn
        for {
          pureUa <- sessionRepository.get(UNCHANGED_SESSION_PREFIX + userAnswersKey)
          _ = logger.info(
            s"[YearAndVersion] Fetching current PSR version by year $year and version ${"%03d".format(current)}"
          )
          currentReturn <- psrRetrievalService.getStandardPsrDetails(
            None,
            Some(year),
            Some("%03d".format(current)),
            controllers.routes.OverviewController.onPageLoad(request.srn)
          )(hc = implicitly, ec = implicitly, request = dataRequest)
        } yield OptionalDataRequest(
          request,
          userAnswers = Some(currentReturn),
          pureUa,
          previousUserAnswers = None,
          Some(year),
          Some(current),
          previousVersion = None
        )
      }
    }

  def currentAndPreviousVersionForYear(
    year: String,
    current: Int,
    previous: Int
  ): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        val emptyUserAnswers = UserAnswers(request.getUserId + request.srn) // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
        val dataRequest: DataRequest[A] = DataRequest[A](request, emptyUserAnswers)
        val userAnswersKey = request.getUserId + request.srn
        for {
          pureUa <- sessionRepository.get(UNCHANGED_SESSION_PREFIX + userAnswersKey)
          _ = logger.info(s"[Compare] Fetching previous PSR version ${"%03d".format(previous)}")
          previousReturn <- psrRetrievalService.getStandardPsrDetails(
            None,
            Some(year),
            Some("%03d".format(previous)),
            controllers.routes.OverviewController.onPageLoad(request.srn),
            fetchingPreviousVersion = true
          )(hc = implicitly, ec = implicitly, request = dataRequest)
          previousUa = if (previousReturn.data == emptyUserAnswers.data) {
            logger.info("[Compare] Fetching previous return was empty, setting previousUserAnswers to None")
            None
          } else {
            logger.info("[Compare] Fetching previous return was ok, setting previousUserAnswers to Some")
            Some(previousReturn)
          }
          _ = logger.info(s"[Compare] Fetching current PSR version ${"%03d".format(current)}")
          currentReturn <- psrRetrievalService.getStandardPsrDetails(
            None,
            Some(year),
            Some("%03d".format(current)),
            controllers.routes.OverviewController.onPageLoad(request.srn)
          )(hc = implicitly, ec = implicitly, request = dataRequest.copy(previousUserAnswers = previousUa))
        } yield OptionalDataRequest(
          request,
          Some(currentReturn),
          pureUa,
          previousUa,
          Some(year),
          Some(current),
          Some(previous)
        )
      }
    }

  def fbNumber(fbNumber: String): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        val emptyUserAnswers = UserAnswers(request.getUserId + request.srn) // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
        val dataRequest: DataRequest[A] = DataRequest[A](request, emptyUserAnswers)
        val userAnswersKey = request.getUserId + request.srn
        for {
          pureUa <- sessionRepository.get(UNCHANGED_SESSION_PREFIX + userAnswersKey)
          _ = logger.info(s"[FBNumber] Fetching current PSR version by fbNumber $fbNumber")
          currentReturn <- psrRetrievalService.getStandardPsrDetails(
            optFbNumber = Some(fbNumber),
            optPeriodStartDate = None,
            optPsrVersion = None,
            controllers.routes.OverviewController.onPageLoad(request.srn)
          )(hc = implicitly, ec = implicitly, request = dataRequest)
        } yield OptionalDataRequest(
          request,
          Some(currentReturn),
          pureUa,
          previousUserAnswers = None,
          None,
          None,
          previousVersion = None
        )
      }
    }
}

@ImplementedBy(classOf[DataRetrievalETMPActionProviderImpl])
trait DataRetrievalETMPActionProvider {
  def currentAndPreviousVersionForYear(
    year: String,
    current: Int,
    previous: Int
  ): ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
  def versionForYear(year: String, current: Int): ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
  def fbNumber(fbNumber: String): ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
}

class DataRetrievalETMPActionProviderImpl @Inject()(
  sessionRepository: SessionRepository,
  psrRetrievalService: PsrRetrievalService
)(implicit val ec: ExecutionContext)
    extends DataRetrievalETMPActionProvider {

  private val dataRetrievalETMPAction = new DataRetrievalETMPAction(sessionRepository, psrRetrievalService)

  def currentAndPreviousVersionForYear(
    year: String,
    current: Int,
    previous: Int
  ): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    dataRetrievalETMPAction.currentAndPreviousVersionForYear(year, current, previous)

  def versionForYear(year: String, current: Int): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    dataRetrievalETMPAction.versionForYear(year, current)

  def fbNumber(fbNumber: String): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    dataRetrievalETMPAction.fbNumber(fbNumber)
}
