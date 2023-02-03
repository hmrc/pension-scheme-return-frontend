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

package controllers.actions

import models.UserAnswers
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataCreationActionImpl @Inject()(sessionRepository: SessionRepository)(implicit val executionContext: ExecutionContext) extends DataCreationAction {

  override protected def transform[A](request: OptionalDataRequest[A]): Future[DataRequest[A]] = {

    request.userAnswers match {
      case None =>
        val userAnswersKey = request.getUserId + request.request.schemeDetails.srn
        val userAnswers = UserAnswers(userAnswersKey)
        sessionRepository.set(userAnswers).map(_ => DataRequest(request.request, userAnswers))
      case Some(data) =>
        Future.successful(DataRequest(request.request, data))
    }
  }
}

trait DataCreationAction extends ActionTransformer[OptionalDataRequest, DataRequest]
