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

import play.api.mvc._
import generators.Generators
import org.scalatest.OptionValues
import models.UserAnswers
import models.requests.{AllowedAccessRequest, OptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject.Inject

class FakeDataRetrievalETMPActionProvider @Inject()(
  userAnswers: Option[UserAnswers],
  pureUserAnswers: Option[UserAnswers],
  previousUserAnswers: Option[UserAnswers]
) extends DataRetrievalETMPActionProvider
    with Generators
    with OptionValues {

  override def fbNumber(fbNumber: String): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] =
        Future(
          OptionalDataRequest(
            request,
            userAnswers,
            pureUserAnswers,
            previousUserAnswers,
            None,
            None,
            None
          )
        )

      override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }

  override def versionForYear(
    year: String,
    current: Int
  ): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] =
        Future(
          OptionalDataRequest(
            request,
            userAnswers,
            pureUserAnswers,
            previousUserAnswers,
            Some(year),
            Some(current),
            if (current - 1 <= 0) None else Some(current - 1)
          )
        )

      override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }

  override def currentAndPreviousVersionForYear(
    year: String,
    current: Int,
    previous: Int
  ): ActionTransformer[AllowedAccessRequest, OptionalDataRequest] =
    new ActionTransformer[AllowedAccessRequest, OptionalDataRequest] {
      override def transform[A](request: AllowedAccessRequest[A]): Future[OptionalDataRequest[A]] =
        Future(
          OptionalDataRequest(
            request,
            userAnswers,
            pureUserAnswers,
            previousUserAnswers,
            Some(year),
            Some(current),
            Some(previous)
          )
        )

      override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
}
