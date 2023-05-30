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
import controllers.nonsipp.memberdetails.upload.FileUploadWrongFormatController._
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.FileUploadWrongFormatPage
import play.api.i18n._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class FileUploadWrongFormatController @Inject()(
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
    Redirect(navigator.nextPage(FileUploadWrongFormatPage(srn), mode, request.userAnswers))
  }
}

object FileUploadWrongFormatController {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadWrongFormat.title",
      heading = "fileUploadWrongFormat.heading",
      description = Some(
        ParagraphMessage(Message("fileUploadWrongFormat.paragraph1")) ++
          ListMessage(
            ListType.Bullet,
            "fileUploadWrongFormat.bullet1",
            "fileUploadWrongFormat.bullet2",
            "fileUploadWrongFormat.bullet3",
            Message("fileUploadWrongFormat.bullet4") ++
              DownloadLinkMessage(
                "fileUploadWrongFormat.bullet4.link",
                controllers.nonsipp.memberdetails.routes.DownloadPensionSchemeTemplateController.downloadFile.url
              )
          ) ++
          ParagraphMessage(Message("fileUploadWrongFormat.paragraph2"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "fileUploadWrongFormat.button",
      onSubmit = routes.FileUploadWrongFormatController.onSubmit(srn, mode)
    )
}
