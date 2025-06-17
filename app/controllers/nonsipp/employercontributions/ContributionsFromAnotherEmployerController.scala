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

package controllers.nonsipp.employercontributions

import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined300, toRefined50}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.employercontributions.ContributionsFromAnotherEmployerPage
import services.SaveService
import config.RefinedTypes.{Max300, Max50}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import models.Mode
import controllers.nonsipp.employercontributions.ContributionsFromAnotherEmployerController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class ContributionsFromAnotherEmployerController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers.fillForm(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), form)
        Ok(view(preparedForm, viewModel(srn, index, secondaryIndex, mode, memberName.fullName)))
      }
    }

  def onSubmit(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
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
                request.userAnswers
                  .set(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), value)
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
