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

package controllers.nonsipp.whatyouwillneedloans

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.whatyouwillneedloans.WhatYouWillNeedLoansPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class WhatYouWillNeedLoansController @Inject()(
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
      Ok(view(WhatYouWillNeedLoansController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedLoansPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedLoansController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeedLoans.title"),
      Message("whatYouWillNeedLoans.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      routes.WhatYouWillNeedLoansController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeedLoans.paragraph1") ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph2") ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph3") ++

          ParagraphMessage("whatYouWillNeedLoans.paragraph4") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLoans.recipient1",
            "whatYouWillNeedLoans.recipient2",
            "whatYouWillNeedLoans.recipient3"
          ) ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph5") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLoans.loansMade1",
            "whatYouWillNeedLoans.loansMade2"
          ) ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph6") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLoans.loansFor1",
            "whatYouWillNeedLoans.loansFor2",
            "whatYouWillNeedLoans.loansFor3",
            "whatYouWillNeedLoans.loansFor4",
            "whatYouWillNeedLoans.loansFor5"
          ) ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph7") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLoans.security"
          ) ++
          ParagraphMessage("whatYouWillNeedLoans.paragraph8") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLoans.arrears"
          )
      )
}
