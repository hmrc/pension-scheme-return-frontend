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

package controllers.nonsipp

import cats.implicits.toShow
import config.Constants.maxMembers
import controllers.actions._
import controllers.nonsipp.HowManyMembersController._
import forms.IntFormProvider
import forms.mappings.errors.IntFormErrors
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.HowManyMembersPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{ListMessage, ListType, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import viewmodels.models.{Field, FormPageViewModel, FurtherDetailsViewModel}
import views.html.IntView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class HowManyMembersController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: IntView,
  dateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = HowManyMembersController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSubmissionEndDate(srn) { submissionEndDate =>
      val page = HowManyMembersPage(srn, request.pensionSchemeId)
      val schemeName = request.schemeDetails.schemeName

      Ok(view(viewModel(srn, schemeName, submissionEndDate, mode, request.userAnswers.fillForm(page, form))))
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
              BadRequest(view(viewModel(srn, schemeName, submissionEndDate, mode, formWithErrors)))
            }
          },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet(page, value))
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
  ): FormPageViewModel[TripleQuestion[Int]] = FormPageViewModel(
    Message("howManyMembers.title", endDate.show),
    Message("howManyMembers.heading", schemeName, endDate.show),
    TripleQuestion(
      form,
      Field("howManyMembers.field1"),
      Field("howManyMembers.field2"),
      Field("howManyMembers.field3"),
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
      )
    ),
    routes.HowManyMembersController.onSubmit(srn, mode)
  )
}
