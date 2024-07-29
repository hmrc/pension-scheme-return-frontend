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
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.Max300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{RemoveMemberContributionPage, TotalMemberContributionPage}
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, MemberState, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveMemberContributionController @Inject()(
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

  private val form = RemoveMemberContributionController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRedirectToTaskList(srn)
          totalContrib <- request.userAnswers.get(TotalMemberContributionPage(srn, index)).getOrRedirectToTaskList(srn)
        } yield {
          Ok(
            view(
              form,
              RemoveMemberContributionController.viewModel(
                srn,
                index: Max300,
                totalContrib,
                nameDOB.fullName
              )
            )
          )
        }
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                total <- request.userAnswers
                  .get(TotalMemberContributionPage(srn, index))
                  .getOrRecoverJourneyT
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemoveMemberContributionController.viewModel(srn, index, total, nameDOB.fullName)
                )
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- request.userAnswers
                  .remove(TotalMemberContributionPage(srn, index))
                  .set(MemberStatus(srn, index), MemberState.Changed)
                  .mapK[Future]
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.membercontributions.routes.MemberContributionListController
                    .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemoveMemberContributionPage(srn, index), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveMemberContributionPage(srn, index), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemoveMemberContributionController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeMemberContributions.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    total: Money,
    fullName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeMemberContributions.title"),
      Message("removeMemberContributions.heading", total.displayAs, fullName),
      routes.RemoveMemberContributionController.onSubmit(srn, index)
    )
}
