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

package controllers.nonsipp.membercontributions

import services.{PsrSubmissionService, SaveService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import viewmodels.implicits._
import pages.nonsipp.membercontributions.MemberContributionsPage
import controllers.nonsipp.membercontributions.MemberContributionsController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class MemberContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = MemberContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(MemberContributionsPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers
                .set(MemberContributionsPage(srn), value)
            )
            _ <- saveService.save(updatedAnswers)
            redirectTo <- if (value) {
              Future.successful(Redirect(navigator.nextPage(MemberContributionsPage(srn), mode, updatedAnswers)))
            } else {
              {
                psrSubmissionService
                  .submitPsrDetails(
                    srn,
                    fallbackCall =
                      controllers.nonsipp.membercontributions.routes.MemberContributionsController.onPageLoad(srn, mode)
                  )(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) => Redirect(navigator.nextPage(MemberContributionsPage(srn), mode, updatedAnswers))
                  }
              }
            }
          } yield redirectTo
      )
  }
}

object MemberContributionsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "memberContributions.error.required"
  )

  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "memberContributions.title",
      "memberContributions.heading",
      routes.MemberContributionsController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("memberContributions.paragraph") ++
        ListMessage(ListType.Bullet, "memberContributions.listItem1", "memberContributions.listItem2")
    )
}
