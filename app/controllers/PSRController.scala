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
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.applicative._
import cats.syntax.either._
import config.Refined.Max3
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import models.{DateRange, UserAnswers}
import models.requests.DataRequest
import play.api.i18n.I18nSupport
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.Result
import queries.{Gettable, Removable, Settable}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

abstract class PSRController extends FrontendBaseController with I18nSupport {

  implicit def requestToUserAnswers(implicit req: DataRequest[_]): UserAnswers = req.userAnswers

  def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[_]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  // Used to specifically refine an index retrieved from user answers
  // These indexes will be strings and 0 based so we need to add 1 before refining
  def refineStringIndex[A](indexAsString: String)(implicit ev: Validate[Int, A]): Option[Refined[Int, A]] =
    indexAsString.toIntOption.flatMap(index => refineV[A](index + 1).toOption)

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

    def getOrRecoverJourney[F[_]: Applicative]: F[Either[Result, A]] = maybe match {
      case Some(value) => value.asRight[Result].pure
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).asLeft[A].pure
    }

    def getOrRecoverJourneyT(implicit ec: ExecutionContext): EitherT[Future, Result, A] = maybe match {
      case Some(value) => EitherT.right(Future.successful(value))
      case None => EitherT.left(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }
  }
  implicit class FutureOps[A](f: Future[A]) {
    def liftF(implicit ec: ExecutionContext): EitherT[Future, Result, A] = EitherT.liftF(f)
  }

  implicit class TryOps[A](t: Try[A]) {
    def toFuture: Future[A] = Future.fromTry(t)
  }

  implicit class ListOps[A](l: List[A]) {
    def zipWithIndexToMap: Map[String, A] =
      l.zipWithIndex
        .map(t => t._2.toString -> t._1)
        .toMap
  }

  implicit class TaxOrAccountingPeriodOps(o: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]) {
    def merge: Option[DateRange] = o.map(_.map(_.toList.map(_._1).sorted.head).merge)
  }

  implicit class UserAnswersOps(userAnswers: UserAnswers) {
    def exists[A: Reads](page: Gettable[A])(f: A => Boolean)(implicit request: DataRequest[_]): Boolean =
      request.userAnswers.get(page).fold(true)(f)
  }

  implicit class UserAnswersTryOps(userAnswers: Try[UserAnswers]) {
    def set[A: Writes](page: Settable[A], value: A): Try[UserAnswers] = userAnswers.flatMap(_.set(page, value))
    def remove[A: Writes](page: Removable[A]): Try[UserAnswers] = userAnswers.flatMap(_.removePages(List(page)))
  }
}
