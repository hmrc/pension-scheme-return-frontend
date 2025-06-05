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

import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.employercontributions.RemoveEmployerContributionsController._
import utils.IntUtils.{toInt, toRefined300, toRefined50}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import pages.nonsipp.employercontributions._
import services.{PsrSubmissionService, SaveService}
import config.RefinedTypes.{Max300, Max50}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, MemberState, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveEmployerContributionsController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = RemoveEmployerContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          total <- request.userAnswers
            .get(TotalEmployerContributionPage(srn, memberIndex, index))
            .getOrRedirectToTaskList(srn)
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRedirectToTaskList(srn)
          employerName <- request.userAnswers
            .get(EmployerNamePage(srn, memberIndex, index))
            .getOrRedirectToTaskList(srn)
        } yield Ok(
          view(form, viewModel(srn, memberIndex: Max300, index: Max50, total, nameDOB.fullName, employerName))
        )
      ).merge
    }

  def onSubmit(srn: Srn, memberIndex: Int, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (
              for {
                total <- request.userAnswers
                  .get(TotalEmployerContributionPage(srn, memberIndex, index))
                  .getOrRecoverJourneyT
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourneyT
                employerName <- request.userAnswers.get(EmployerNamePage(srn, memberIndex, index)).getOrRecoverJourneyT
              } yield BadRequest(
                view(formWithErrors, viewModel(srn, memberIndex, index, total, nameDOB.fullName, employerName))
              )
            ).merge,
          removeDetails =>
            if (removeDetails) {
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers
                    .remove(EmployerNamePage(srn, memberIndex, index))
                    .remove(EmployerContributionsProgress(srn, memberIndex, index))
                    .set(MemberStatus(srn, memberIndex), MemberState.Changed)
                )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall =
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                      .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(_ =>
                Redirect(
                  navigator
                    .nextPage(RemoveEmployerContributionsPage(srn, memberIndex), NormalMode, updatedAnswers)
                )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveEmployerContributionsPage(srn, memberIndex), NormalMode, request.userAnswers)
                  )
                )
            }
        )
    }
}

object RemoveEmployerContributionsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeEmployerContributions.error.required"
  )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max50,
    total: Money,
    fullName: String,
    employerName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeEmployerContributions.title"),
      Message("removeEmployerContributions.heading", total.displayAs, employerName, fullName),
      routes.RemoveEmployerContributionsController.onSubmit(srn, memberIndex, index)
    )
}
