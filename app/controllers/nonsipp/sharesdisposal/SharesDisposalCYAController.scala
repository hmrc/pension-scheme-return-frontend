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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import models.PointOfEntry.NoPointOfEntry
import utils.nonsipp.summary.SharesDisposalCheckAnswersUtils
import utils.IntUtils.{toRefined50, toRefined5000}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models.SectionJourneyStatus.Completed
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesDisposalCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      if (request.userAnswers.get(SharesDisposalProgress(srn, shareIndex, disposalIndex)).contains(Completed)) {
        // Clear any PointOfEntry
        saveService
          .save(
            request.userAnswers
              .set(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), NoPointOfEntry)
              .getOrElse(request.userAnswers)
          )
      }
      onPageLoadCommon(srn, shareIndex, disposalIndex, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    shareIndex: Int,
    disposalIndex: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, shareIndex, disposalIndex, mode)
    }

  def onPageLoadCommon(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    if (
      !request.userAnswers
        .get(SharesDisposalProgress(srn, shareIndex, disposalIndex))
        .exists(_.completed)
    ) {
      Future.successful(Redirect(routes.SharesDisposalListController.onPageLoad(srn, 1)))
    } else {
      SharesDisposalCheckAnswersUtils
        .summaryDataAsyncT(srn, (shareIndex, disposalIndex), mode)
        .map { data =>
          Ok(view(SharesDisposalCheckAnswersUtils.viewModel(data)))
        }
        .merge
    }

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- request.userAnswers
          .set(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
            .onPageLoad(srn, shareIndex, disposalIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(SharesDisposalCYAPage(srn), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
