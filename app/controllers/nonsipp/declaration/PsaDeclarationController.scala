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

package controllers.nonsipp.declaration

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import controllers.PSRController
import config.Constants.{RETURN_PERIODS, SUBMISSION_DATE}
import controllers.actions._
import navigation.Navigator
import models.{NormalMode, UserAnswers}
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.declaration.PsaDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.ExecutionContext

import javax.inject.{Inject, Named}

class PsaDeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: ContentPageView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(view(PsaDeclarationController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        _ <- psrSubmissionService.submitPsrDetails(srn = srn, isSubmitted = true)
        _ <- saveService.save(UserAnswers(request.userAnswers.id))
      } yield {
        Redirect(navigator.nextPage(PsaDeclarationPage(srn), NormalMode, request.userAnswers))
          .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
          .addingToSession((SUBMISSION_DATE, schemeDateService.submissionDateAsString(schemeDateService.now())))
      }
    }
}

object PsaDeclarationController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("psaDeclaration.title"),
      Message("psaDeclaration.heading"),
      ContentPageViewModel(),
      routes.PsaDeclarationController.onSubmit(srn)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        ParagraphMessage("psaDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "psaDeclaration.listItem1",
            "psaDeclaration.listItem2",
            "psaDeclaration.listItem3"
          )
      )
}
