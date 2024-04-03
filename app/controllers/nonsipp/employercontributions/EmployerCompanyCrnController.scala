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

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.Refined.{Max300, Max50}
import controllers.PSRController
import cats.implicits.catsSyntaxApplicativeId
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.employercontributions.EmployerCompanyCrnController._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Crn, Mode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import pages.nonsipp.employercontributions.{EmployerCompanyCrnPage, EmployerNamePage}
import services.SaveService
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class EmployerCompanyCrnController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Either[String, Crn]] = EmployerCompanyCrnController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(EmployerNamePage(srn, memberIndex, index)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(EmployerCompanyCrnPage(srn, memberIndex, index), form)
        Ok(view(preparedForm, viewModel(srn, memberIndex, index, mode, companyName)))
      }
    }

  def onSubmit(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(EmployerNamePage(srn, memberIndex, index))
              .getOrRecoverJourney(
                companyName => BadRequest(view(formWithErrors, viewModel(srn, memberIndex, index, mode, companyName)))
              )
              .pure[Future],
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo(value))
                .mapK
              nextPage = navigator.nextPage(EmployerCompanyCrnPage(srn, memberIndex, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, memberIndex, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object EmployerCompanyCrnController {

  private val noFormErrors = InputFormErrors.input(
    "employerCompanyCRN.no.conditional.error.required",
    "employerCompanyCRN.no.conditional.error.invalid",
    "employerCompanyCRN.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "employerCompanyCRN.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.crn(
        "employerCompanyCRN.yes.conditional.error.required",
        "employerCompanyCRN.yes.conditional.error.invalid",
        "employerCompanyCRN.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max50,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "employerCompanyCRN.title",
      Message("employerCompanyCRN.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("employerCompanyCRN.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("employerCompanyCRN.no.conditional", companyName), FieldType.Textarea)
      ).withHint(
        Message("employerCompanyCRN.hint.part1") ++
          Message("employerCompanyCRN.hint.crn").speakAs("employerCompanyCRN.hint.crn.speakAs") ++
          Message("employerCompanyCRN.hint.part2")
      ),
      controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
        .onSubmit(srn, memberIndex, index, mode)
    )
}
