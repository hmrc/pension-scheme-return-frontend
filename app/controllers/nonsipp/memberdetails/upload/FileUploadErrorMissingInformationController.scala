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

import viewmodels.implicits._
import play.api.mvc._
import pages.FileUploadErrorMissingInformationPage
import controllers.actions._
import navigation.Navigator
import controllers.nonsipp.memberdetails.upload.FileUploadErrorMissingInformationController._
import models.Mode
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class FileUploadErrorMissingInformationController @Inject() (
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
    Redirect(navigator.nextPage(FileUploadErrorMissingInformationPage(srn), mode, request.userAnswers))
  }
}

object FileUploadErrorMissingInformationController {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadErrorMissingInformation.title",
      heading = "fileUploadErrorMissingInformation.heading",
      description = Some(
        ParagraphMessage(Message("fileUploadErrorMissingInformation.paragraph1")) ++
          ListMessage(
            ListType.Bullet,
            "fileUploadErrorMissingInformation.bullet1",
            "fileUploadErrorMissingInformation.bullet2",
            "fileUploadErrorMissingInformation.bullet3",
            Message("fileUploadErrorMissingInformation.bullet4") ++
              DownloadLinkMessage(
                "fileUploadErrorMissingInformation.bullet4.link",
                controllers.nonsipp.memberdetails.routes.DownloadPensionSchemeTemplateController.downloadFile.url
              )
          ) ++
          ParagraphMessage(Message("fileUploadErrorMissingInformation.paragraph2"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "fileUploadErrorMissingInformation.button",
      onSubmit = routes.FileUploadErrorMissingInformationController.onSubmit(srn, mode)
    )
}
