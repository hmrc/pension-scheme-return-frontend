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

package controllers.nonsipp.receivetransfer

import config.Refined.{Max300, Max5}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.receivetransfer.RemoveTransferInController._
import forms.YesNoPageFormProvider
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.receivetransfer.{transferInPages, RemoveTransferInPage, TransferringSchemeNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveTransferInController @Inject()(
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

  private val form = RemoveTransferInController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max5): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
          transferringSchemeName <- request.userAnswers
            .get(TransferringSchemeNamePage(srn, memberIndex, index))
            .getOrRecoverJourney
        } yield Ok(
          view(form, viewModel(srn, memberIndex: Max300, index: Max5, nameDOB.fullName, transferringSchemeName))
        )
      ).merge
    }

  def onSubmit(srn: Srn, memberIndex: Max300, index: Max5): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourneyT
                transferringSchemeName <- request.userAnswers
                  .get(TransferringSchemeNamePage(srn, memberIndex, index))
                  .getOrRecoverJourneyT
              } yield BadRequest(
                view(formWithErrors, viewModel(srn, memberIndex, index, nameDOB.fullName, transferringSchemeName))
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .removePages(
                        transferInPages(srn, memberIndex, index)
                      )
                  )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedAnswers)
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemoveTransferInPage(srn, memberIndex), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveTransferInPage(srn, memberIndex), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemoveTransferInController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeTransferIn.error.required"
  )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max5,
    fullName: String,
    transferringSchemeName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeTransferIn.title"),
      Message("removeTransferIn.heading", transferringSchemeName, fullName),
      routes.RemoveTransferInController.onSubmit(srn, memberIndex, index)
    )
}
