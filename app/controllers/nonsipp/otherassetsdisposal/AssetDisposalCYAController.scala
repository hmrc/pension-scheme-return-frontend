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

package controllers.nonsipp.otherassetsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal._
import play.api.mvc._
import utils.nonsipp.summary.OtherAssetsDisposalCheckAnswersUtils
import utils.IntUtils.{toRefined50, toRefined5000}
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AssetDisposalCYAController @Inject() (
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
    index: Int,
    disposalIndex: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, index, disposalIndex, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    if (
      !request.userAnswers
        .get(OtherAssetsDisposalProgress(srn, index, disposalIndex))
        .exists(_.completed)
    ) {
      Future.successful(Redirect(routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, 1)))
    } else {
      OtherAssetsDisposalCheckAnswersUtils
        .summaryDataAsyncT(srn, (index, disposalIndex), mode)
        .map { data =>
          Ok(view(OtherAssetsDisposalCheckAnswersUtils.viewModel(data)))
        }
        .merge
    }

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- request.userAnswers
          .set(OtherAssetsDisposalProgress(srn, index, disposalIndex), SectionJourneyStatus.Completed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, index, disposalIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(OtherAssetsDisposalCYAPage(srn), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
