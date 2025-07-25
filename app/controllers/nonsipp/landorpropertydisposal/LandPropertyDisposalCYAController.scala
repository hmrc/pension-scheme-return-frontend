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

package controllers.nonsipp.landorpropertydisposal

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import utils.nonsipp.summary.LandOrPropertyDisposalCheckAnswersUtils
import utils.IntUtils.{toRefined50, toRefined5000}
import pages.nonsipp.landorpropertydisposal._
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n._
import viewmodels.models._
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandPropertyDisposalCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    disposalIndex: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      onPageLoadCommon(srn, index, disposalIndex, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    landOrPropertyIndex: Int,
    disposalIndex: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, landOrPropertyIndex, disposalIndex, mode)
    }

  def onPageLoadCommon(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    if (
      !request.userAnswers
        .get(LandOrPropertyDisposalProgress(srn, landOrPropertyIndex, disposalIndex))
        .exists(_.completed)
    )
      Future.successful(Redirect(routes.LandOrPropertyDisposalListController.onPageLoad(srn, 1)))
    else {
      val landOrPropertyCheckDisposalAnswersUtils = LandOrPropertyDisposalCheckAnswersUtils(saveService)
      landOrPropertyCheckDisposalAnswersUtils
        .summaryDataAsync(srn, (landOrPropertyIndex, disposalIndex), mode)
        .map(_.map { data =>
          Ok(view(landOrPropertyCheckDisposalAnswersUtils.viewModel(data)))
        })
        .merge
    }

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers.set(LandPropertyDisposalCompletedPage(srn, index, disposalIndex), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedUserAnswers,
            fallbackCall = controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
              .onPageLoad(srn, index, disposalIndex, mode)
          )(using implicitly, implicitly, request = DataRequest(request.request, updatedUserAnswers))
          .map {
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case Some(_) =>
              Redirect(
                navigator
                  .nextPage(
                    LandPropertyDisposalCompletedPage(srn, index, disposalIndex),
                    NormalMode,
                    updatedUserAnswers
                  )
              )
          }
      } yield redirectTo
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
