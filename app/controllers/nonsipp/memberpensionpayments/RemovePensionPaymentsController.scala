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

package controllers.nonsipp.memberpensionpayments

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, IntOpts}
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.{RemovePensionPaymentsPage, TotalAmountPensionPaymentsPage}
import controllers.actions.IdentifyAndRequireData
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemovePensionPaymentsController @Inject()(
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

  private val form = RemovePensionPaymentsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index.refined)).getOrRedirectToTaskList(srn)
          totalAmountPensionPayment <- request.userAnswers
            .get(TotalAmountPensionPaymentsPage(srn, index.refined))
            .getOrRedirectToTaskList(srn)
        } yield {
          Ok(
            view(
              form,
              RemovePensionPaymentsController.viewModel(
                srn,
                index.refined,
                totalAmountPensionPayment,
                nameDOB.fullName
              )
            )
          )
        }
      ).merge
    }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                total <- request.userAnswers
                  .get(TotalAmountPensionPaymentsPage(srn, index.refined))
                  .getOrRecoverJourneyT
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index.refined)).getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemovePensionPaymentsController.viewModel(srn, index.refined, total, nameDOB.fullName)
                )
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .remove(TotalAmountPensionPaymentsPage(srn, index.refined))
                      .set(MemberStatus(srn, index.refined), MemberState.Changed)
                  )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                    .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemovePensionPaymentsPage(srn, index.refined), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemovePensionPaymentsPage(srn, index.refined), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemovePensionPaymentsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removePensionPayments.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    total: Money,
    fullName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removePensionPayments.title"),
      Message("removePensionPayments.heading", total.displayAs, fullName),
      routes.RemovePensionPaymentsController.onSubmit(srn, index)
    )
}
