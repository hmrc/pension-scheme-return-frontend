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

import viewmodels.implicits._
import play.api.mvc._
import pages.FileUploadTooManyRowsPage
import controllers.actions._
import controllers.nonsipp.memberdetails.upload.FileUploadTooManyRowsController._
import navigation.Navigator
import models.Mode
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class FileUploadTooManyRowsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(view(viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(FileUploadTooManyRowsPage(srn), mode, request.userAnswers))
  }
}

object FileUploadTooManyRowsController {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadTooManyRows.title",
      heading = "fileUploadTooManyRows.heading",
      description = Some(
        ParagraphMessage("fileUploadTooManyRows.paragraph1") ++ ParagraphMessage("fileUploadTooManyRows.paragraph2")
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadTooManyRowsController.onSubmit(srn, mode)
    )
}
