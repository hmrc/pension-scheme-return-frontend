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

package controllers.nonsipp.receivetransfer

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import pages.nonsipp.receivetransfer.ReportAnotherTransferInPage
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.nonsipp.receivetransfer.ReportAnotherTransferInController._
import config.Refined.{Max300, Max5}
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class ReportAnotherTransferInController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = ReportAnotherTransferInController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers.fillForm(ReportAnotherTransferInPage(srn, index, secondaryIndex), form)
        Ok(view(preparedForm, viewModel(srn, index, secondaryIndex, mode, memberName.fullName)))
      }
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, secondaryIndex, mode, memberName.fullName))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers
                  .set(ReportAnotherTransferInPage(srn, index, secondaryIndex), value)
              )
              _ <- saveService.save(updatedAnswers)
              submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedAnswers)
            } yield submissionResult.getOrRecoverJourney(
              _ =>
                Redirect(
                  navigator
                    .nextPage(ReportAnotherTransferInPage(srn, index, secondaryIndex), mode, updatedAnswers)
                )
            )
        )
    }
}

object ReportAnotherTransferInController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "reportAnotherTransferIn.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    mode: Mode,
    memberName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "reportAnotherTransferIn.title",
      Message("reportAnotherTransferIn.heading", memberName),
      YesNoPageViewModel(
        hint = Some("reportAnotherTransferIn.hint")
      ),
      routes.ReportAnotherTransferInController.onSubmit(srn, index, secondaryIndex, mode)
    )

}
