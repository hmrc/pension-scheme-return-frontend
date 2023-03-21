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

import cats.implicits.toShow
import config.Constants.maxMembers
import controllers.HowManyMembersController._
import controllers.actions._
import forms.TripleIntFormProvider
import forms.mappings.errors.IntFormErrors
import models.{Mode, SchemeMemberNumbers}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.HowManyMembersPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.TripleIntViewModel
import views.html.TripleIntView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HowManyMembersController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TripleIntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TripleIntView,
  dateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = HowManyMembersController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSubmissionEndDate(srn) { submissionEndDate =>
      val preparedForm = form.fromUserAnswers(HowManyMembersPage(srn, request.pensionSchemeId))
      val schemeName = request.schemeDetails.schemeName

      Future.successful(Ok(view(preparedForm, viewModel(srn, schemeName, submissionEndDate, mode))))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSubmissionEndDate(srn) { submissionEndDate =>
      val schemeName = request.schemeDetails.schemeName
      val page = HowManyMembersPage(srn, request.pensionSchemeId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, viewModel(srn, schemeName, submissionEndDate, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(page, mode, updatedAnswers))
        )
    }
  }

  private def usingSubmissionEndDate(srn: Srn)(body: LocalDate => Future[Result])(implicit request: DataRequest[_]) =
    dateService
      .schemeEndDate(srn)
      .fold(
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
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

  def form(formProvider: TripleIntFormProvider): Form[SchemeMemberNumbers] = formProvider.schemeMembers(
    field1Errors,
    field2Errors,
    field3Errors
  )

  def viewModel(srn: Srn, schemeName: String, endDate: LocalDate, mode: Mode): TripleIntViewModel = TripleIntViewModel(
    Message("howManyMembers.title", endDate.show),
    Message("howManyMembers.heading", schemeName, endDate.show),
    "howManyMembers.field1",
    "howManyMembers.field2",
    "howManyMembers.field3",
    controllers.routes.HowManyMembersController.onSubmit(srn, mode)
  )
}
