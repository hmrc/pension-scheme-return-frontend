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

package controllers.nonsipp.memberdetails

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails._
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.Max300
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.nonsipp.SoftDelete
import forms.YesNoPageFormProvider
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import controllers.nonsipp.memberdetails.RemoveMemberDetailsController._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveMemberDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  submissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController
    with SoftDelete {

  private val form = RemoveMemberDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      withMemberDetails(srn, index)(nameDOB => Ok(view(form, viewModel(srn, index, nameDOB, mode))))
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              withMemberDetails(srn, index)(
                nameDOB => BadRequest(view(formWithErrors, viewModel(srn, index, nameDOB, mode)))
              )
            ),
          removeMemberDetails => {
            if (removeMemberDetails) {
              for {
                updatedAnswers <- softDeleteMember(srn, index).mapK[Future]
                _ <- saveService.save(updatedAnswers)
                result <- submissionService
                  .submitPsrDetailsWithUA(srn, updatedAnswers, routes.PensionSchemeMembersController.onPageLoad(srn))
              } yield result.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemoveMemberDetailsPage(srn), mode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemoveMemberDetailsPage(srn), mode, request.userAnswers)))
            }
          }
        )
    }

  private def withMemberDetails(srn: Srn, index: Max300)(
    f: NameDOB => Result
  )(implicit request: DataRequest[_]): Result =
    (
      for {
        nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRedirectToTaskList(srn)
      } yield {
        f(nameDOB)
      }
    ).merge
}
object RemoveMemberDetailsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeMemberDetails.error.required"
  )

  def viewModel(srn: Srn, index: Max300, nameDOB: NameDOB, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("removeMemberDetails.title", nameDOB.fullName),
      Message("removeMemberDetails.heading", nameDOB.fullName),
      YesNoPageViewModel(
        hint = Some(Message("removeMemberDetails.hint"))
      ),
      routes.RemoveMemberDetailsController.onSubmit(srn, index, mode)
    )
}
