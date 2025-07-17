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

package controllers.nonsipp.otherassetsheld

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import utils.nonsipp.summary.OtherAssetsCheckAnswersUtils
import utils.IntUtils.toRefined5000
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import pages.nonsipp.otherassetsheld._
import models.PointOfEntry.NoPointOfEntry
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.models.SectionJourneyStatus.Completed
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherAssetsCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      if (request.userAnswers.get(OtherAssetsProgress(srn, index)).contains(Completed)) {
        // Clear any PointOfEntry
        saveService.save(
          request.userAnswers
            .set(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
            .getOrElse(request.userAnswers)
        )
      }
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
    request.userAnswers.get(OtherAssetsProgress(srn, index)) match {
      case Some(value) if value.inProgress =>
        Redirect(controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, 1, mode))
      case _ =>
        OtherAssetsCheckAnswersUtils
          .summaryData(srn, index, mode)
          .map { data =>
            Ok(
              view(
                OtherAssetsCheckAnswersUtils.viewModel(
                  data.parameters,
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
      val prePopulated = request.userAnswers.get(OtherAssetsPrePopulated(srn, index))
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers
            .set(OtherAssetsHeldPage(srn), true)
            .setWhen(prePopulated.isDefined)(OtherAssetsPrePopulated(srn, index), true)
            .set(OtherAssetsCompleted(srn, index), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedUserAnswers,
            fallbackCall =
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, mode)
          )
          .map {
            case None => controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(_) => navigator.nextPage(OtherAssetsCYAPage(srn), NormalMode, request.userAnswers)
          }
      } yield Redirect(redirectTo)
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
