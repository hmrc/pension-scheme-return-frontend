/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.nonsipp.memberdetails.upload

import controllers.actions._
import controllers.nonsipp.memberdetails.upload.CheckingMemberDetailsFileController._
import models.{Mode, NormalMode, UploadKey, UploadStatus}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.CheckingMemberDetailsFilePage
import play.api.i18n._
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.ContentPageViewModel
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class CheckingMemberDetailsFileController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(view(viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).map {
      case None | Some(UploadStatus.InProgress) => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(UploadStatus.Failed) => redirectNextPage(srn, uploadSuccessful = false, mode)
      case Some(_: UploadStatus.Success) => redirectNextPage(srn, uploadSuccessful = true, mode)
    }
  }

  private def redirectNextPage(srn: Srn, uploadSuccessful: Boolean, mode: Mode)(implicit req: DataRequest[_]): Result =
    Redirect(navigator.nextPage(CheckingMemberDetailsFilePage(srn, uploadSuccessful), mode, req.userAnswers))
}

object CheckingMemberDetailsFileController {
  def viewModel(srn: Srn, mode: Mode): ContentPageViewModel = ContentPageViewModel(
    title = "checkingMemberDetailsFile.title",
    heading = "checkingMemberDetailsFile.heading",
    contents = List(ParagraphMessage("checkingMemberDetailsFile.paragraph")),
    isStartButton = false,
    buttonText = "site.continue",
    isLargeHeading = true,
    onSubmit = routes.CheckingMemberDetailsFileController.onSubmit(srn, mode)
  )
}
