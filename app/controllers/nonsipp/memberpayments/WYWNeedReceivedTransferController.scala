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

package controllers.nonsipp.memberpayments

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberpayments.WYWNeedReceivedTransferPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView
import viewmodels.implicits._

import javax.inject.{Inject, Named}

//What You Will Need - Received Transfers.
class WYWNeedReceivedTransferController @Inject()(
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
      Ok(view(WYWNeedReceivedTransferController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WYWNeedReceivedTransferPage(srn), NormalMode, request.userAnswers))
    }
}

object WYWNeedReceivedTransferController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("WYWNeedReceivedTransfer.title"),
      Message("WYWNeedReceivedTransfer.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      controllers.nonsipp.memberpayments.routes.WYWNeedReceivedTransferController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("WYWNeedReceivedTransfer.paragraph1") ++
          ParagraphMessage("WYWNeedReceivedTransfer.paragraph2") ++
          ListMessage(
            ListType.Bullet,
            "WYWNeedReceivedTransfer.tellUs1",
            "WYWNeedReceivedTransfer.tellUs2",
            "WYWNeedReceivedTransfer.tellUs3",
            "WYWNeedReceivedTransfer.tellUs4",
            "WYWNeedReceivedTransfer.tellUs5",
            "WYWNeedReceivedTransfer.tellUs6",
            "WYWNeedReceivedTransfer.tellUs7"
          ) ++
          ParagraphMessage("WYWNeedReceivedTransfer.paragraph3") ++
          ParagraphMessage("WYWNeedReceivedTransfer.paragraph4")
      )
}
