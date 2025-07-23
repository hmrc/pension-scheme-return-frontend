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

package controllers.nonsipp.moneyborrowed

import services.PsrSubmissionService
import play.api.mvc._
import utils.nonsipp.summary.MoneyBorrowedCheckAnswersUtils
import utils.IntUtils.toRefined5000
import controllers.actions._
import navigation.Navigator
import models._
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import play.api.i18n._
import pages.nonsipp.moneyborrowed._
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class MoneyBorrowedCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, index, mode)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(MoneyBorrowedProgress(srn, index)) match {
      case Some(InProgress(_)) =>
        Redirect(
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, 1, mode)
        )
      case _ =>
        MoneyBorrowedCheckAnswersUtils
          .summaryData(srn, index, mode)
          .map { data =>
            Ok(
              view(
                MoneyBorrowedCheckAnswersUtils.viewModel(
                  data
                )
              )
            )
          }
          .merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          fallbackCall =
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, index, mode)
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) => Redirect(navigator.nextPage(MoneyBorrowedCYAPage(srn), NormalMode, request.userAnswers))
        }
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
