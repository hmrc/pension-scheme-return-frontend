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

package controllers.nonsipp.memberdetails.upload

import services.UploadService
import viewmodels.implicits._
import play.api.mvc._
import controllers.actions._
import controllers.nonsipp.memberdetails.upload.FileUploadErrorController._
import navigation.Navigator
import pages.nonsipp.memberdetails.upload.FileUploadErrorPage
import models._
import models.requests.DataRequest
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.ExecutionContext

import javax.inject.{Inject, Named}

class FileUploadErrorController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  uploadService: UploadService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).map {
      case Some(UploadFormatError) | Some(_: UploadErrors) | Some(UploadMaxRowsError) => Ok(view(viewModel(srn, mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).map {
      case Some(UploadFormatError) => navToNextPage(srn, mode, UploadFormatError)
      case Some(UploadMaxRowsError) => navToNextPage(srn, mode, UploadMaxRowsError)
      case Some(uploadErrors: UploadErrors) => navToNextPage(srn, mode, uploadErrors)
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def navToNextPage(srn: Srn, mode: Mode, error: UploadError)(implicit
    request: DataRequest[_]
  ): Result =
    Redirect(navigator.nextPage(FileUploadErrorPage(srn, error), mode, request.userAnswers))
}

object FileUploadErrorController {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      title = "fileUploadError.title",
      heading = "fileUploadError.heading",
      description = Some(ParagraphMessage(Message("fileUploadError.paragraph"))),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.continue",
      onSubmit = routes.FileUploadErrorController.onSubmit(srn, mode)
    )
}
