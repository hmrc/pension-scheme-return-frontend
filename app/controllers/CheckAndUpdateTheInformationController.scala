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

package controllers

import pages.nonsipp.schemedesignatory.HowManyMembersPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.WhatYouWillNeedPage
import controllers.actions._
import navigation.Navigator
import models.{CheckMode, NormalMode}
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.Future

import javax.inject.{Inject, Named}

class CheckAndUpdateTheInformationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, taxYear: String, version: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(CheckAndUpdateTheInformationController.viewModel(srn, taxYear, version)))
    }

  def onSubmit(srn: Srn, taxYear: String, version: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      // as we cannot access pensionSchemeId in the navigator
      val members = request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
      if (members.exists(_.totalActiveAndDeferred > 99)) {
        Future.successful(
          Redirect(controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode))
        )
      } else {
        Future.successful(Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, request.userAnswers)))
      }
    }
}

object CheckAndUpdateTheInformationController {

  def viewModel(srn: Srn, taxYear: String, version: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("checkAndUpdateTheInformation.title"),
      Message("checkAndUpdateTheInformation.heading"),
      ContentPageViewModel(isStartButton = true),
      routes.CheckAndUpdateTheInformationController.onSubmit(srn, taxYear, version)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("checkAndUpdateTheInformation.paragraph1") ++
          ParagraphMessage("checkAndUpdateTheInformation.paragraph2") ++
          Heading2.medium("checkAndUpdateTheInformation.legend") ++
          ParagraphMessage("checkAndUpdateTheInformation.paragraph3")
      )
}
