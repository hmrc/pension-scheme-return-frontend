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

package controllers.nonsipp.shares

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import pages.nonsipp
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Mode, NormalMode}
import controllers.nonsipp.shares.RemoveSharesController._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import pages.nonsipp.shares._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveSharesController @Inject()(
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

  private val form = RemoveSharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          companyName <- request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRedirectToTaskList(srn)
        } yield Ok(
          view(form, viewModel(srn, index, mode, companyName))
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                companyName <- request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourneyT
              } yield BadRequest(
                view(formWithErrors, viewModel(srn, index, mode, companyName))
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              val isLast = request.userAnswers.map(CompanyNameRelatedSharesPages(srn)).size == 1
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(nonsipp.shares.sharesPages(srn, index, isLast)))
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, 1, mode)
                )
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemoveSharesPage(srn, index), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveSharesPage(srn, index), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemoveSharesController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeShares.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeShares.title"),
      Message("removeShares.heading", companyName),
      routes.RemoveSharesController.onSubmit(srn, index, mode)
    )
}
