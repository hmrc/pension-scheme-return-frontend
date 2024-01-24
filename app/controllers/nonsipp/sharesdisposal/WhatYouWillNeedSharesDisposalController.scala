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

package controllers.nonsipp.sharesdisposal

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.sharesdisposal.WhatYouWillNeedSharesDisposalPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView
import viewmodels.implicits._

import javax.inject.{Inject, Named}

class WhatYouWillNeedSharesDisposalController @Inject()(
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
      Ok(view(WhatYouWillNeedSharesDisposalController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedSharesDisposalPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedSharesDisposalController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("sharesDisposal.whatYouWillNeed.title"),
      Message("sharesDisposal.whatYouWillNeed.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      controllers.nonsipp.sharesdisposal.routes.WhatYouWillNeedSharesDisposalController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("sharesDisposal.whatYouWillNeed.paragraph1") ++
          ParagraphMessage("sharesDisposal.whatYouWillNeed.paragraph2") ++
          ListMessage(
            ListType.Bullet,
            "sharesDisposal.whatYouWillNeed.bullet1",
            "sharesDisposal.whatYouWillNeed.bullet2",
            "sharesDisposal.whatYouWillNeed.bullet3"
          ) ++
          ParagraphMessage("sharesDisposal.whatYouWillNeed.paragraph3") ++
          ListMessage(
            ListType.Bullet,
            "sharesDisposal.whatYouWillNeed.bullet4",
            "sharesDisposal.whatYouWillNeed.bullet5",
            "sharesDisposal.whatYouWillNeed.bullet6",
            "sharesDisposal.whatYouWillNeed.bullet7",
            "sharesDisposal.whatYouWillNeed.bullet8",
            "sharesDisposal.whatYouWillNeed.bullet9",
            "sharesDisposal.whatYouWillNeed.bullet10",
            "sharesDisposal.whatYouWillNeed.bullet11"
          ) ++
          ParagraphMessage("sharesDisposal.whatYouWillNeed.paragraph4") ++
          ListMessage(
            ListType.Bullet,
            "sharesDisposal.whatYouWillNeed.bullet12",
            "sharesDisposal.whatYouWillNeed.bullet13",
            "sharesDisposal.whatYouWillNeed.bullet14"
          )
      )
}
