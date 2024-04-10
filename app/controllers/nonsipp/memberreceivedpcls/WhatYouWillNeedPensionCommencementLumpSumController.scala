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

package controllers.nonsipp.memberreceivedpcls

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models.NormalMode
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.WhatYouWillNeedPensionCommencementLumpSumPage
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class WhatYouWillNeedPensionCommencementLumpSumController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(view(WhatYouWillNeedPensionCommencementLumpSumController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedPensionCommencementLumpSumPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedPensionCommencementLumpSumController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("WhatYouWillNeed.PensionCommencementLumpSum.title", SchemeName),
      Message("WhatYouWillNeed.PensionCommencementLumpSum.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      routes.WhatYouWillNeedPensionCommencementLumpSumController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("WhatYouWillNeed.PensionCommencementLumpSum.paragraph1") ++
          ParagraphMessage("WhatYouWillNeed.PensionCommencementLumpSum.paragraph2") ++
          ListMessage(
            ListType.Bullet,
            "WhatYouWillNeed.PensionCommencementLumpSum.totalAmount",
            "WhatYouWillNeed.PensionCommencementLumpSum.relevantPension"
          ) ++
          ParagraphMessage("WhatYouWillNeed.PensionCommencementLumpSum.paragraph3")
      )
}
