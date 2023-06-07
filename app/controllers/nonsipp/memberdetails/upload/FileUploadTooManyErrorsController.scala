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

import cats.data.NonEmptyList
import controllers.actions._
import controllers.nonsipp.memberdetails.upload.FileUploadTooManyErrorsController._
import models.{Mode, UploadErrors, UploadKey, ValidationError, ValidationErrorType}
import models.SchemeId.Srn
import models.ValidationErrorType.ValidationErrorType
import navigation.Navigator
import pages.FileUploadTooManyErrorsPage
import play.api.i18n._
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.LabelSize
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class FileUploadTooManyErrorsController @Inject()(
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
      case Some(UploadErrors(errors)) => Ok(view(viewModel(srn, errors.size, errors.map(_.errorType), mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(FileUploadTooManyErrorsPage(srn), mode, request.userAnswers))
  }
}

object FileUploadTooManyErrorsController {
  def viewModel(
    srn: Srn,
    numErrors: Int,
    errorTypes: NonEmptyList[ValidationErrorType],
    mode: Mode
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadTooManyErrors.title",
      heading = "fileUploadTooManyErrors.heading",
      description = Some(
        ParagraphMessage("fileUploadTooManyErrors.paragraph") ++
          Heading2("fileUploadTooManyErrors.heading2", LabelSize.Medium) ++
          ParagraphMessage(Message("fileUploadTooManyErrors.paragraph2", numErrors)) ++
          errorList(errorTypes)
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn, mode)
    )

  private def errorList(errorTypes: NonEmptyList[ValidationErrorType]): ListMessage = {
    val errorMessages = errorTypes.map {
      case ValidationErrorType.FirstName => "fileUploadTooManyErrors.firstName"
      case ValidationErrorType.LastName => "fileUploadTooManyErrors.lastName"
      case ValidationErrorType.DateOfBirth => "fileUploadTooManyErrors.dateOfBirth"
      case ValidationErrorType.DuplicateNino => "fileUploadTooManyErrors.duplicateNino"
      case ValidationErrorType.NoNinoReason => "fileUploadTooManyErrors.noNinoReason"
      case ValidationErrorType.NinoFormat => "fileUploadTooManyErrors.ninoFormat"
    }.distinct

    ListMessage.Bullet(errorMessages.head, errorMessages.tail.map(Message(_)): _*)
  }
}
