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

package controllers.nonsipp.otherassetsheld

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.WhatYouWillNeedOtherAssetsPage
import controllers.actions._
import navigation.Navigator
import models.NormalMode
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.HeadingSize
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class WhatYouWillNeedOtherAssetsController @Inject() (
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
      Ok(view(WhatYouWillNeedOtherAssetsController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedOtherAssetsPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedOtherAssetsController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("otherAssets.whatYouWillNeed.title"),
      Message("otherAssets.whatYouWillNeed.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      controllers.nonsipp.otherassetsheld.routes.WhatYouWillNeedOtherAssetsController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("otherAssets.whatYouWillNeed.paragraph1") ++
          ParagraphMessage("otherAssets.whatYouWillNeed.paragraph2") ++
          ParagraphMessage("otherAssets.whatYouWillNeed.paragraph3") ++
          ListMessage(
            ListType.Bullet,
            "otherAssets.whatYouWillNeed.paragraph3.listItem1",
            "otherAssets.whatYouWillNeed.paragraph3.listItem2",
            "otherAssets.whatYouWillNeed.paragraph3.listItem3",
            "otherAssets.whatYouWillNeed.paragraph3.listItem4",
            "otherAssets.whatYouWillNeed.paragraph3.listItem5"
          ) ++
          Heading2("otherAssets.whatYouWillNeed.heading.paragraph4", HeadingSize.Medium) ++
          ParagraphMessage("otherAssets.whatYouWillNeed.paragraph4") ++
          ListMessage(
            ListType.Bullet,
            "otherAssets.whatYouWillNeed.paragraph4.listItem1",
            "otherAssets.whatYouWillNeed.paragraph4.listItem2"
          ) ++
          Heading2("otherAssets.whatYouWillNeed.heading.paragraph5", HeadingSize.Medium) ++
          ParagraphMessage("otherAssets.whatYouWillNeed.paragraph5") ++
          ListMessage(
            ListType.Bullet,
            "otherAssets.whatYouWillNeed.paragraph5.listItem1",
            "otherAssets.whatYouWillNeed.paragraph5.listItem2",
            "otherAssets.whatYouWillNeed.paragraph5.listItem3",
            "otherAssets.whatYouWillNeed.paragraph5.listItem4"
          )
      )
}
