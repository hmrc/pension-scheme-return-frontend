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
import com.google.inject.Inject
import controllers.routes
import models.SchemeId.Srn
import play.api.Logger
import models.{Mode, ViewOnlyMode}
import models.requests.DataRequest
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

class IdentifyAndRequireData @Inject()(
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  getDataFromETMP: DataRetrievalETMPActionProvider,
  saveData: DataSavingAction,
  requireData: DataRequiredAction,
  playBodyParsers: PlayBodyParsers
)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)

  private val errorAction = new ActionBuilder[DataRequest, AnyContent] {
    override def invokeBlock[A](request: Request[A], block: DataRequest[A] => Future[Result]): Future[Result] =
      Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

    override def parser: BodyParser[AnyContent] = playBodyParsers.default

    override protected def executionContext: ExecutionContext = ec
  }

  /**
   * Populates UserAnswers in the request context from Mongo
   * If pure (Initial user answers / ETMP GET snapshot) and previous version are available, populate the request context with those too
   **/
  def apply(srn: Srn): ActionBuilder[DataRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData)

  /**
   * if mode is ViewOnlyMode
   * - Populates UserAnswers and previous version UserAnswers in the request context from ETMP
   * - if not: same as apply(srn: Srn)
   **/
  def apply(srn: Srn, mode: Mode, year: String, current: Int, previous: Int): ActionBuilder[DataRequest, AnyContent] =
    if (mode == ViewOnlyMode) {
      if (previous == 0) {
        identify
          .andThen(allowAccess(srn))
          .andThen(getDataFromETMP.versionForYear(year, current))
          .andThen(requireData)
          .andThen(saveData)
      } else {
        identify
          .andThen(allowAccess(srn))
          .andThen(getDataFromETMP.currentAndPreviousVersionForYear(year, current, previous))
          .andThen(requireData)
          .andThen(saveData)
      }
    } else {
      apply(srn)
    }

  /**
   * Populates UserAnswers and previous version UserAnswers in the request context from ETMP using the fbNumber
   **/
  def apply(srn: Srn, fbNumber: String): ActionBuilder[DataRequest, AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getDataFromETMP.fbNumber(fbNumber))
      .andThen(requireData)
      .andThen(saveData)

  /**
   * Populates UserAnswers in the request context from ETMP
   * If current is greater than 1, also fetch the previous return and add it to the DataRequest context as previous version UserAnswers
   */
  def apply(srn: Srn, year: String, current: String): ActionBuilder[DataRequest, AnyContent] =
    current.toIntOption match {
      case Some(1) =>
        identify
          .andThen(allowAccess(srn))
          .andThen(getDataFromETMP.versionForYear(year, 1))
          .andThen(requireData)
          .andThen(saveData)
      case Some(currentVersion) if currentVersion > 1 =>
        identify
          .andThen(allowAccess(srn))
          .andThen(getDataFromETMP.currentAndPreviousVersionForYear(year, currentVersion, currentVersion - 1))
          .andThen(requireData)
          .andThen(saveData)
      // current version was not a number
      case None =>
        logger.error(s"current PSA version $current is not a valid version number")
        errorAction
    }
}
