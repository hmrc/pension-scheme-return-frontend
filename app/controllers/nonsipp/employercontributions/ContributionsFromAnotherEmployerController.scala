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

package controllers.nonsipp.employercontributions

import config.Refined.{Max300, Max50}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.employercontributions.ContributionsFromAnotherEmployerController._
import pages.nonsipp.unquotedshares.UnquotedSharesPage
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.employercontributions.{ContributionsFromAnotherEmployerPage, EmployerNamePage}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ContributionsFromAnotherEmployerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = ContributionsFromAnotherEmployerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
//      memberName <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        //      request.usingAnswer(EmployerNamePage(srn: Srn, index, secondaryIndex)).sync { employerName =>
        val preparedForm =
          request.userAnswers.fillForm(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), form)
        Ok(view(preparedForm, viewModel(srn, index, secondaryIndex, mode, memberName.fullName)))
        //      }
      }
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, secondaryIndex, mode, memberName.fullName))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object ContributionsFromAnotherEmployerController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "contributionsFromAnotherEmployer.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
    mode: Mode,
    memberName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "contributionsFromAnotherEmployer.title",
      Message("contributionsFromAnotherEmployer.heading", memberName),
      YesNoPageViewModel(
        hint = Some("contributionsFromAnotherEmployer.hint")
      ),
      routes.ContributionsFromAnotherEmployerController.onSubmit(srn, index, secondaryIndex, mode)
    )

}
