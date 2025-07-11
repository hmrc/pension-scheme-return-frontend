/*
 * Copyright 2025 HM Revenue & Customs
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
import config.FrontendAppConfig
import handlers.PsrLockedException
import play.api.Logger
import repositories.SessionRepository
import models.requests.AllowedAccessRequest

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class DataCheckLockingActionImpl @Inject() (
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig
)(implicit val executionContext: ExecutionContext)
    extends DataCheckLockingAction {

  private val logger = Logger(getClass)

  override protected def transform[A](request: AllowedAccessRequest[A]): Future[AllowedAccessRequest[A]] =
    if (appConfig.lockingEnabled) {
      for {
        uaBySrn <- sessionRepository.getBySrnAndIdNotEqual(request.getUserId, request.srn)
      } yield uaBySrn match {
        case None => request
        case Some(ua) =>
          logger.warn(
            s"[Locking] Locking detected for srn ${request.srn}. User answers already exist with last updated time: ${ua.lastUpdated}"
          )
          throw PsrLockedException(request.srn)
      }
    } else {
      Future.successful(request)
    }
}

trait DataCheckLockingAction extends ActionTransformer[AllowedAccessRequest, AllowedAccessRequest]
