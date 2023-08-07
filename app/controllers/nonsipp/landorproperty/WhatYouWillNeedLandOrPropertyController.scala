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

package controllers.nonsipp.landorproperty

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorproperty.WhatYouWillNeedLandOrPropertyPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class WhatYouWillNeedLandOrPropertyController @Inject()(
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
      Ok(view(WhatYouWillNeedLandOrPropertyController.viewModel(srn, request.schemeDetails.schemeName)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedLandOrPropertyPage(srn), NormalMode, request.userAnswers))
    }
}

object WhatYouWillNeedLandOrPropertyController {

  def viewModel(srn: Srn, SchemeName: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeedLandOrProperty.title"),
      Message("whatYouWillNeedLandOrProperty.heading", SchemeName),
      ContentPageViewModel(isLargeHeading = true),
      routes.WhatYouWillNeedLandOrPropertyController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph1") ++
          ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph2") ++
          ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph3") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLandOrProperty.info1",
            "whatYouWillNeedLandOrProperty.info2",
            "whatYouWillNeedLandOrProperty.info3",
            "whatYouWillNeedLandOrProperty.info4",
            "whatYouWillNeedLandOrProperty.info5",
            "whatYouWillNeedLandOrProperty.info6"
          ) ++
          ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph4") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLandOrProperty.isLeasedInfo1",
            "whatYouWillNeedLandOrProperty.isLeasedInfo2",
            "whatYouWillNeedLandOrProperty.isLeasedInfo3",
            "whatYouWillNeedLandOrProperty.isLeasedInfo4"
          ) ++
          ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph5") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLandOrProperty.isAcquisitionOrContributionInfo1",
            "whatYouWillNeedLandOrProperty.isAcquisitionOrContributionInfo2"
          ) ++
          ParagraphMessage("whatYouWillNeedLandOrProperty.paragraph6") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeedLandOrProperty.isAcquisitionInfo1",
            "whatYouWillNeedLandOrProperty.isAcquisitionInfo2",
            "whatYouWillNeedLandOrProperty.isAcquisitionInfo3"
          )
      )
}
