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
import controllers.nonsipp.memberdetails.upload.FileUploadErrorSummaryController._
import viewmodels.implicits._
import play.api.mvc._
import pages.FileUploadErrorSummaryPage
import controllers.actions._
import navigation.Navigator
import models._
import cats.data.NonEmptyList
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.HeadingSize
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.ExecutionContext

import javax.inject.{Inject, Named}

class FileUploadErrorSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).map {
      case Some(UploadErrors(errors)) => Ok(view(viewModel(srn, errors, mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(FileUploadErrorSummaryPage(srn), mode, request.userAnswers))
  }
}

object FileUploadErrorSummaryController {

  private def errorSummary(errors: NonEmptyList[ValidationError]): TableMessage = {
    val errorList = errors.map { err =>
      Message(err.key) -> Message(err.message)
    }
    TableMessage(
      content = errorList,
      heading = Some(Message("site.cell") -> Message("site.error"))
    )
  }

  def viewModel(srn: Srn, errors: NonEmptyList[ValidationError], mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadErrorSummary.title",
      heading = "fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", HeadingSize.Medium) ++
          errorSummary(errors)
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, mode)
    )
}
