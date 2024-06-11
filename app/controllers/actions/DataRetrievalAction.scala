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

import play.api.mvc.ActionTransformer
import config.Constants.UNCHANGED_SESSION_PREFIX
import repositories.SessionRepository
import models.requests.{AllowedAccessRequest, OptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class DataRetrievalActionImpl @Inject()(
  val sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  override protected def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] = {
    val userAnswersKey = request.getUserId + request.srn
    for {
      ua <- sessionRepository.get(userAnswersKey)
      pureUa <- sessionRepository.get(UNCHANGED_SESSION_PREFIX + userAnswersKey)
    } yield OptionalDataRequest(request, ua, pureUa, None)
  }
}

trait DataRetrievalAction extends ActionTransformer[AllowedAccessRequest, OptionalDataRequest]
