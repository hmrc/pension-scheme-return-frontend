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

import play.api.mvc.{ActionRefiner, Result}
import controllers.routes
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class DataRequiredActionImpl @Inject() (implicit val executionContext: ExecutionContext) extends DataRequiredAction {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    request.userAnswers match {
      case None =>
        Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))
      case Some(data) =>
        Future.successful(
          Right(
            DataRequest(
              request = request.request,
              userAnswers = data,
              pureUserAnswers = request.pureUserAnswers,
              previousUserAnswers = request.previousUserAnswers,
              year = request.year,
              currentVersion = request.currentVersion,
              previousVersion = request.previousVersion
            )
          )
        )
    }
}

trait DataRequiredAction extends ActionRefiner[OptionalDataRequest, DataRequest]
