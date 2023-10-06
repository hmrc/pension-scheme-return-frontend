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

package controllers.nonsipp.moneyborrowed

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.WhatYouWillNeedLandPropertyDisposalPage
import pages.nonsipp.moneyborrowed.WhatYouWillNeedMoneyBorrowedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class WhatYouWillNeedMoneyBorrowedController @Inject()(
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
      Ok(view(WhatYouWillNeedMoneyBorrowedController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedMoneyBorrowedPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedMoneyBorrowedController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeedMoneyBorrowed.title"),
      Message("whatYouWillNeedMoneyBorrowed.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      routes.WhatYouWillNeedMoneyBorrowedController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeedMoneyBorrowed.paragraph1") ++
          ParagraphMessage("whatYouWillNeedMoneyBorrowed.paragraph2") ++
          ParagraphMessage("whatYouWillNeedMoneyBorrowed.paragraph3") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedMoneyBorrowed.list1",
            "whatYouWillNeedMoneyBorrowed.list2",
            "whatYouWillNeedMoneyBorrowed.list3",
            "whatYouWillNeedMoneyBorrowed.list4",
            "whatYouWillNeedMoneyBorrowed.list5",
            "whatYouWillNeedMoneyBorrowed.list6",
            "whatYouWillNeedMoneyBorrowed.list7"
          )
      )
}
