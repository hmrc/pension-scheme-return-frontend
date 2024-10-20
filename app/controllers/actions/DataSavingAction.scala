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
import com.google.inject.ImplementedBy
import config.Constants.{PREPOP_PREFIX, PREVIOUS_SUBMITTED_PREFIX, UNCHANGED_SESSION_PREFIX}
import repositories.SessionRepository
import models.requests.DataRequest

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class DataSavingActionImpl @Inject()(sessionRepository: SessionRepository)(
  implicit val executionContext: ExecutionContext
) extends DataSavingAction {

  override protected def transform[A](request: DataRequest[A]): Future[DataRequest[A]] = {
    val userAnswersKey = request.getUserId + request.srn
    for {
      _ <- sessionRepository.set(request.userAnswers)
      _ <- sessionRepository.set(request.userAnswers.copy(id = UNCHANGED_SESSION_PREFIX + userAnswersKey))
      _ <- request.previousUserAnswers.fold(Future.unit)(
        previousUserAnswers =>
          sessionRepository.set(previousUserAnswers.copy(id = PREVIOUS_SUBMITTED_PREFIX + userAnswersKey))
      )
      _ <- request.prePopUserAnswers.fold(Future.unit)(
        prePopUserAnswers => sessionRepository.set(prePopUserAnswers.copy(id = PREPOP_PREFIX + userAnswersKey))
      )
    } yield request
  }
}

@ImplementedBy(classOf[DataSavingActionImpl])
trait DataSavingAction extends ActionTransformer[DataRequest, DataRequest]
