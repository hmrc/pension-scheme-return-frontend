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
import controllers.nonsipp.employercontributions.RemoveEmployerContributionsController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.employercontributions.{
  EmployerNamePage,
  RemoveEmployerContributionsPage,
  TotalEmployerContributionPage
}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveEmployerContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = RemoveEmployerContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          total <- request.userAnswers.get(TotalEmployerContributionPage(srn, memberIndex, index)).getOrRecoverJourney
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
          employerName <- request.userAnswers.get(EmployerNamePage(srn, memberIndex, index)).getOrRecoverJourney
        } yield Ok(
          view(form, viewModel(srn, memberIndex: Max300, index: Max50, total, nameDOB.fullName, employerName))
        )
      ).merge
    }

  def onSubmit(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
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
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(EmployerNamePage(srn, memberIndex, index)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator
                  .nextPage(RemoveEmployerContributionsPage(srn), mode, updatedAnswers)
              )
            } else {
              Future
                .successful(
                  Redirect(navigator.nextPage(RemoveEmployerContributionsPage(srn), mode, request.userAnswers))
                )
            }
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
