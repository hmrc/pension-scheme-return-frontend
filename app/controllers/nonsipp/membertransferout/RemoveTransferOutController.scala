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

package controllers.nonsipp.membertransferout

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined300, toRefined5}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.NormalMode
import play.api.data.Form
import config.RefinedTypes.{Max300, Max5}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import pages.nonsipp.membertransferout._
import play.api.i18n.{I18nSupport, MessagesApi}
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveTransferOutController @Inject()(
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

  private val form = RemoveTransferOutController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRedirectToTaskList(srn)
          receivingSchemeName <- request.userAnswers
            .get(ReceivingSchemeNamePage(srn, memberIndex, index))
            .getOrRedirectToTaskList(srn)
        } yield {
          Ok(
            view(
              form,
              RemoveTransferOutController
                .viewModel(srn, memberIndex: Max300, index: Max5, nameDOB.fullName, receivingSchemeName)
            )
          )
        }
      ).merge
    }

  def onSubmit(srn: Srn, memberIndex: Int, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourneyT
                receivingSchemeName <- request.userAnswers
                  .get(ReceivingSchemeNamePage(srn, memberIndex, index))
                  .getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemoveTransferOutController.viewModel(srn, memberIndex, index, nameDOB.fullName, receivingSchemeName)
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
                      .removeOnlyMultiplePages(transferOutPages(srn, memberIndex, index))
                      .set(MemberStatus(srn, memberIndex), MemberState.Changed)
                  )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
                    .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemoveTransferOutPage(srn, memberIndex), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveTransferOutPage(srn, memberIndex), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemoveTransferOutController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "transferOut.removeTransferOut.error.required"
  )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max5,
    fullName: String,
    receivingSchemeName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("transferOut.removeTransferOut.title"),
      Message("transferOut.removeTransferOut.heading", receivingSchemeName, fullName),
      routes.RemoveTransferOutController.onSubmit(srn, memberIndex, index)
    )
}
