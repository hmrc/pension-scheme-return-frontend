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

package controllers.nonsipp.memberdetails

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.HowToUploadPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{DownloadLinkMessage, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, PageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class HowToUploadController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(HowToUploadController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(HowToUploadPage(srn), NormalMode, request.userAnswers))
    }
}

object HowToUploadController {

  def viewModel(srn: Srn): PageViewModel[ContentPageViewModel] =
    PageViewModel(
      Message("howToUpload.title"),
      Message("howToUpload.heading"),
      ContentPageViewModel(isLargeHeading = true),
      routes.HowToUploadController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("howToUpload.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "howToUpload.name",
            "howToUpload.nino",
            "howToUpload.dob"
          ) ++
          ParagraphMessage(
            DownloadLinkMessage(
              "howToUpload.linkMessage",
              routes.DownloadPensionSchemeTemplateController.downloadFile.url
            ),
            "howToUpload.linkMessage1"
          )
      )
}
