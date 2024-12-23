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

package controllers.nonsipp.receivetransfer

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.receivetransfer.WhatYouWillNeedReceivedTransferPage
import controllers.actions._
import navigation.Navigator
import models.NormalMode
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class WhatYouWillNeedReceivedTransferController @Inject()(
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
      Ok(view(WhatYouWillNeedReceivedTransferController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedReceivedTransferPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedReceivedTransferController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("WhatYouWillNeed.ReceivedTransfer.title"),
      Message("WhatYouWillNeed.ReceivedTransfer.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      controllers.nonsipp.receivetransfer.routes.WhatYouWillNeedReceivedTransferController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("WhatYouWillNeed.ReceivedTransfer.paragraph1") ++
          ListMessage(
            ListType.Bullet,
            "WhatYouWillNeed.ReceivedTransfer.tellUs1",
            "WhatYouWillNeed.ReceivedTransfer.tellUs2",
            "WhatYouWillNeed.ReceivedTransfer.tellUs3",
            "WhatYouWillNeed.ReceivedTransfer.tellUs4",
            "WhatYouWillNeed.ReceivedTransfer.tellUs5",
            "WhatYouWillNeed.ReceivedTransfer.tellUs6",
            "WhatYouWillNeed.ReceivedTransfer.tellUs7"
          ) ++
          ParagraphMessage("WhatYouWillNeed.ReceivedTransfer.paragraph2")
      )
}
