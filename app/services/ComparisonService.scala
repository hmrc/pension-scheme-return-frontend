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

import play.api.mvc.AnyContent
import config.Constants.{COMPARE_PREVIOUS_PREFIX, UNCHANGED_SESSION_PREFIX}
import repositories.SessionRepository
import models.UserAnswers
import models.requests.DataRequest

import scala.concurrent.Future

import javax.inject.Inject

class ComparisonService @Inject()(
  sessionRepository: SessionRepository
) {
  def getPureUserAnswers()(
    implicit request: DataRequest[AnyContent]
  ): Future[Option[UserAnswers]] = {
    val currentUA = request.userAnswers
    sessionRepository.get(UNCHANGED_SESSION_PREFIX + currentUA.id)
  }

  def getPreviousAnswers()(
    implicit request: DataRequest[AnyContent]
  ): Future[Option[UserAnswers]] = {
    val currentUA = request.userAnswers
    sessionRepository.get(COMPARE_PREVIOUS_PREFIX + currentUA.id)
  }
}
