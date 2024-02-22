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

package controllers.nonsipp.shares

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.RemoveSharesController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.nonsipp
import pages.nonsipp.shares.{
  CompanyNameRelatedSharesPage,
  CompanyNameRelatedSharesPages,
  RemoveSharesPage,
  SharesJourneyStatus
}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, SectionStatus, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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
          companyName <- requiredPage(CompanyNameRelatedSharesPage(srn, index))
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
                  .fromTry(
                    request.userAnswers
                      .remove(
                        nonsipp.shares.sharesPages(srn, index, isLast)
                      )
                      .set(SharesJourneyStatus(srn), SectionStatus.InProgress)
                  )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedAnswers)
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
