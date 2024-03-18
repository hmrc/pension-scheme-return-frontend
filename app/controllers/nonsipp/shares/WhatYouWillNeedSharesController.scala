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

package controllers.nonsipp.shares

import viewmodels.implicits._
import controllers.actions._
import navigation.Navigator
import models.NormalMode
import pages.nonsipp.shares.WhatYouWillNeedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class WhatYouWillNeedSharesController @Inject()(
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
      Ok(view(WhatYouWillNeedSharesController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedSharesPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedSharesController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("shares.whatYouWillNeed.title"),
      Message("shares.whatYouWillNeed.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("shares.whatYouWillNeed.paragraph1") ++
          ParagraphMessage("shares.whatYouWillNeed.paragraph2") ++
          ParagraphMessage("shares.whatYouWillNeed.paragraph3") ++
          ListMessage(
            ListType.Bullet,
            "shares.whatYouWillNeed.paragraph3.listItem1",
            "shares.whatYouWillNeed.paragraph3.listItem2",
            "shares.whatYouWillNeed.paragraph3.listItem3",
            "shares.whatYouWillNeed.paragraph3.listItem4",
            "shares.whatYouWillNeed.paragraph3.listItem5",
            "shares.whatYouWillNeed.paragraph3.listItem6",
            "shares.whatYouWillNeed.paragraph3.listItem7",
            "shares.whatYouWillNeed.paragraph3.listItem8",
            "shares.whatYouWillNeed.paragraph3.listItem9"
          ) ++
          ParagraphMessage("shares.whatYouWillNeed.paragraph4") ++
          ListMessage(
            ListType.Bullet,
            "shares.whatYouWillNeed.paragraph4.listItem1"
          ) ++
          ParagraphMessage("shares.whatYouWillNeed.paragraph5") ++
          ListMessage(
            ListType.Bullet,
            "shares.whatYouWillNeed.paragraph5.listItem1",
            "shares.whatYouWillNeed.paragraph5.listItem2",
            "shares.whatYouWillNeed.paragraph5.listItem3",
            "shares.whatYouWillNeed.paragraph5.listItem4"
          )
      )
}
