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

package controllers.nonsipp.bonds

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds._
import play.api.mvc._
import utils.nonsipp.summary.BondsCheckAnswersUtils
import controllers.PSRController
import utils.IntUtils.toRefined5000
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n._
import models.requests.DataRequest
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class UnregulatedOrConnectedBondsHeldCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Int, mode: Mode)
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
      onPageLoadCommon(srn: Srn, index: Int, mode: Mode)
    }

  def onPageLoadCommon(srn: Srn, index: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(BondsProgress(srn, index)) match {
      case Some(InProgress(_)) => Redirect(routes.BondsListController.onPageLoad(srn, 1, mode))
      case _ =>
        BondsCheckAnswersUtils
          .summaryData(srn, index, mode)
          .map { data =>
            Ok(
              view(
                BondsCheckAnswersUtils.viewModel(
                  data.srn,
                  data.index,
                  data.schemeName,
                  data.nameOfBonds,
                  data.whyDoesSchemeHoldBonds,
                  data.whenDidSchemeAcquireBonds,
                  data.costOfBonds,
                  data.bondsFromConnectedParty,
                  data.areBondsUnregulated,
                  data.incomeFromBonds,
                  data.mode,
                  data.viewOnlyUpdated,
                  data.optYear,
                  data.optCurrentVersion,
                  data.optPreviousVersion
                )
              )
            )
          }
          .merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(BondPrePopulated(srn, index))
      for {
        updatedAnswers <- request.userAnswers
          .set(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .setWhen(prePopulated.isDefined)(BondPrePopulated(srn, index), true)
          .mapK
        _ <- saveService.save(updatedAnswers)
        result <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall =
              controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController.onPageLoad(srn, index, mode)
          )
      } yield result match {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldCYAPage(srn), NormalMode, request.userAnswers))
      }
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
