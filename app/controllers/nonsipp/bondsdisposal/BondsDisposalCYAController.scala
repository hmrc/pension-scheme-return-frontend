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

package controllers.nonsipp.bondsdisposal

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import models.PointOfEntry.NoPointOfEntry
import utils.nonsipp.summary.BondsDisposalCheckAnswersUtils
import utils.IntUtils.{toRefined50, toRefined5000}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models._
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionJourneyStatus.Completed
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class BondsDisposalCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, bondIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      if (request.userAnswers.get(BondsDisposalProgress(srn, bondIndex, disposalIndex)).contains(Completed)) {
        saveService.save(
          request.userAnswers
            .set(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex), NoPointOfEntry)
            .getOrElse(request.userAnswers)
        )
      }
      onPageLoadCommon(srn, bondIndex, disposalIndex, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    bondIndex: Int,
    disposalIndex: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, bondIndex, disposalIndex, mode)
    }

  def onPageLoadCommon(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    if (
      !request.userAnswers
        .get(BondsDisposalProgress(srn, bondIndex, disposalIndex))
        .exists(_.completed)
    ) {
      Future.successful(Redirect(routes.ReportBondsDisposalListController.onPageLoad(srn, 1)))
    } else {
      BondsDisposalCheckAnswersUtils
        .summaryDataAsyncT(srn, (bondIndex, disposalIndex), mode)
        .map(data => Ok(view(BondsDisposalCheckAnswersUtils.viewModel(data))))
        .merge
    }

  def onSubmit(srn: Srn, bondIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        _ <- saveService.save(request.userAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          request.userAnswers,
          fallbackCall = controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
            .onPageLoad(srn, bondIndex, disposalIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(BondsDisposalCYAPage(srn), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
