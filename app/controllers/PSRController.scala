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

package controllers

import cats.Applicative
import cats.data.NonEmptyList
import cats.syntax.applicative._
import config.Refined.Max3
import models.DateRange
import models.requests.DataRequest
import play.api.i18n.I18nSupport
import play.api.libs.json.Reads
import play.api.mvc.Result
import queries.Gettable
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

abstract class PSRController extends FrontendBaseController with I18nSupport {

  def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[_]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  implicit class OptionOps[A](maybe: Option[A]) {
    def getOrRecoverJourney[F[_]: Applicative](f: A => F[Result]): F[Result] = maybe match {
      case Some(value) => f(value)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).pure
    }

    def getOrRecoverJourney(f: A => Result): Result = maybe match {
      case Some(value) => f(value)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

    def getOrRecoverJourney: Either[Result, A] = maybe match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  implicit class TaxOrAccountingPeriodOps(o: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]) {
    def merge: Option[DateRange] = o.map(_.map(_.last._1).merge)
  }
}
