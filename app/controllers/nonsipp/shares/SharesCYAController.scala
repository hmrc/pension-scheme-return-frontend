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

package controllers.nonsipp.shares

import services.{PsrSubmissionService, SaveService}
import utils.nonsipp.summary.SharesCheckAnswersUtils
import utils.IntUtils.toRefined5000
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n._
import viewmodels.models._
import models.requests.DataRequest
import pages.nonsipp.shares._
import play.api.mvc._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesCYAController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
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
    request.userAnswers.get(SharesProgress(srn, index)) match {
      case Some(value) if value.inProgress =>
        Redirect(
          controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, 1, mode)
        )
      case _ =>
        SharesCheckAnswersUtils
          .summaryData(srn, index, mode)
          .map { data =>
            Ok(
              view(
                SharesCheckAnswersUtils.viewModel(
                  data
                )
              )
            )
          }
          .merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(SharePrePopulated(srn, index)).isDefined

      for {
        updatedAnswers <- Future
          .fromTry(
            request.userAnswers
              .set(DidSchemeHoldAnySharesPage(srn), true)
              .set(SharesCompleted(srn, index), SectionCompleted)
              .setWhen(prePopulated)(SharePrePopulated(srn, index), true)
          )
        _ <- saveService.save(updatedAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall = controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, mode)
          )
          .map {
            case None => controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(_) => navigator.nextPage(SharesCYAPage(srn), NormalMode, request.userAnswers)
          }
      } yield Redirect(redirectTo)
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.shares.routes.SharesListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

case class ViewModelParameters(
)
object SharesCYAController {}
