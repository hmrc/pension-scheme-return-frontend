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

package controllers.nonsipp.landorproperty

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import utils.nonsipp.summary.LandOrPropertyCheckAnswersUtils
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.IntUtils.toRefined5000
import pages.nonsipp.landorproperty._
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
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
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadCommon(srn: SchemeId.Srn, index: Max5000, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    request.userAnswers.get(LandOrPropertyProgress(srn, index)) match {
      case Some(value) if value.inProgress =>
        Redirect(
          controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, 1, mode)
        )
      case _ =>
        LandOrPropertyCheckAnswersUtils
          .landOrPropertySummaryData(srn, index, mode)
          .map { data =>
            Ok(
              view(
                LandOrPropertyCheckAnswersUtils.viewModel(
                  data
                )
              )
            )
          }
          .merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(LandOrPropertyPrePopulated(srn, index))
      for {
        updatedAnswers <- request.userAnswers
          .setWhen(request.userAnswers.get(LandOrPropertyHeldPage(srn)).isEmpty)(LandOrPropertyHeldPage(srn), true)
          .setWhen(prePopulated.isDefined)(LandOrPropertyPrePopulated(srn, index), true)
          .mapK[Future]
        _ <- saveService.save(updatedAnswers)
        result <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall =
              controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onPageLoad(srn, index, mode)
          )
      } yield result.getOrRecoverJourney(_ =>
        Redirect(
          navigator
            .nextPage(LandOrPropertyCYAPage(srn), NormalMode, updatedAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
