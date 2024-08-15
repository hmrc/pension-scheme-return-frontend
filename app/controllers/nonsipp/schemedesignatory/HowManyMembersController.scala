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

package controllers.nonsipp.schemedesignatory

import services.{SaveService, SchemeDateService}
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import viewmodels.implicits._
import play.api.mvc._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import controllers.PSRController
import cats.implicits.toShow
import config.Constants.maxMembers
import controllers.actions._
import forms.IntFormProvider
import play.api.i18n.MessagesApi
import forms.mappings.errors.IntFormErrors
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import controllers.nonsipp.schemedesignatory.HowManyMembersController._
import pages.nonsipp.BasicDetailsCompletedPage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models.Mode
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{ListMessage, ListType, Message}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class HowManyMembersController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  dateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = HowManyMembersController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSubmissionEndDate(srn) { submissionEndDate =>
      val page = HowManyMembersPage(srn, request.pensionSchemeId)
      val schemeName = request.schemeDetails.schemeName

      Ok(view(request.userAnswers.fillForm(page, form), viewModel(srn, schemeName, submissionEndDate, mode, form)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    val page = HowManyMembersPage(srn, request.pensionSchemeId)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful {
            usingSubmissionEndDate(srn) { submissionEndDate =>
              BadRequest(view(formWithErrors, viewModel(srn, schemeName, submissionEndDate, mode, form)))
            }
          },
        value =>
          for {
            updatedAnswers <- request.userAnswers
              .transformAndSet(page, value)
              .set(BasicDetailsCompletedPage(srn), SectionCompleted)
              .mapK[Future]
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(page, mode, updatedAnswers))
      )
  }

  private def usingSubmissionEndDate(srn: Srn)(body: LocalDate => Result)(implicit request: DataRequest[_]) =
    dateService
      .schemeEndDate(srn)
      .fold(
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      )(body)
}

object HowManyMembersController {

  private val field1Errors: IntFormErrors =
    IntFormErrors(
      "howManyMembers.field1.error.required",
      "howManyMembers.field1.error.invalid",
      (maxMembers, "howManyMembers.field1.error.max")
    )

  private val field2Errors: IntFormErrors =
    IntFormErrors(
      "howManyMembers.field2.error.required",
      "howManyMembers.field2.error.invalid",
      (maxMembers, "howManyMembers.field2.error.max")
    )

  private val field3Errors: IntFormErrors =
    IntFormErrors(
      "howManyMembers.field3.error.required",
      "howManyMembers.field3.error.invalid",
      (maxMembers, "howManyMembers.field3.error.max")
    )

  def form(formProvider: IntFormProvider): Form[(Int, Int, Int)] = formProvider(
    field1Errors,
    field2Errors,
    field3Errors
  )

  def viewModel(
    srn: Srn,
    schemeName: String,
    endDate: LocalDate,
    mode: Mode,
    form: Form[(Int, Int, Int)]
  ): FormPageViewModel[TripleQuestion[Int, Int, Int]] = FormPageViewModel(
    Message("howManyMembers.title", endDate.show),
    Message("howManyMembers.heading", schemeName, endDate.show),
    TripleQuestion(
      form,
      QuestionField.input("howManyMembers.field1"),
      QuestionField.input("howManyMembers.field2"),
      QuestionField.input("howManyMembers.field3")
    ),
    Option(
      FurtherDetailsViewModel(
        Message("howManyMembers.detailsComponentTitle"),
        ListMessage(
          ListType.Bullet,
          "howManyMembers.List1",
          "howManyMembers.List2",
          "howManyMembers.List3"
        )
      )
    ),
    routes.HowManyMembersController.onSubmit(srn, mode)
  )
}
