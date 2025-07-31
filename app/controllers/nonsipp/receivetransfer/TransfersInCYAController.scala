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

package controllers.nonsipp.receivetransfer

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MemberStatus
import play.api.mvc._
import utils.nonsipp.summary.TransfersInCheckAnswersUtils
import utils.IntUtils.toRefined300
import pages.nonsipp.receivetransfer._
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes._
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TransfersInCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
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

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    TransfersInCheckAnswersUtils
      .summaryData(srn, index, mode)
      .map { data =>
        data.journeys match {
          case Nil => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case journeys =>
            Ok(view(TransfersInCheckAnswersUtils.viewModel(data)))
        }
      }
      .merge

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val transfersInChanged: Boolean =
        request.userAnswers.changedList(_.buildTransfersIn(srn, index))

      for {
        updatedUserAnswers <- request.userAnswers
          .setWhen(transfersInChanged)(MemberStatus(srn, index), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall =
            controllers.nonsipp.receivetransfer.routes.TransfersInCYAController.onPageLoad(srn, index, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(navigator.nextPage(TransfersInCYAPage(srn), mode, updatedUserAnswers))
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
