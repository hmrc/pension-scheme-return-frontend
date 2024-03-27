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

package controllers

import services.{PsrRetrievalService, SaveService}
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

import scala.concurrent.ExecutionContext

import javax.inject.{Inject, Named}

class WhatYouWillNeedController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  psrRetrievalService: PsrRetrievalService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fbNumber: String, taxYear: String, version: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)) { implicit request =>
      Ok(view(WhatYouWillNeedController.viewModel(srn, fbNumber, taxYear, version)))
    }

  def onSubmit(srn: Srn, fbNumber: String, taxYear: String, version: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      for {
        updatedUserAnswers <- fbNumber match {
          case fb if !fb.isBlank => psrRetrievalService.getStandardPsrDetails(Some(fb), None, None)
          case _ => psrRetrievalService.getStandardPsrDetails(None, Some(taxYear), Some(version))
        }
        _ <- saveService.save(updatedUserAnswers)
      } yield {
        val members = updatedUserAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
        if (members.exists(_.total > 99)) { // as we cannot access pensionSchemeId in the navigator
          Redirect(controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode))
        } else {
          Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, updatedUserAnswers))
        }
      }
    }
}

object WhatYouWillNeedController {

  def viewModel(srn: Srn, fbNumber: String, taxYear: String, version: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeed.title"),
      Message("whatYouWillNeed.heading"),
      ContentPageViewModel(isStartButton = true, isLargeHeading = false),
      routes.WhatYouWillNeedController.onSubmit(srn, fbNumber, taxYear, version)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeed.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeed.listItem1",
            "whatYouWillNeed.listItem2",
            "whatYouWillNeed.listItem3",
            "whatYouWillNeed.listItem4"
          ) ++
          ParagraphMessage("whatYouWillNeed.paragraph1") ++
          ParagraphMessage("whatYouWillNeed.paragraph2")
      )
}
