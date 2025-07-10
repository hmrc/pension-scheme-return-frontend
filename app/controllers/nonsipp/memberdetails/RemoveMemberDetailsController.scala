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
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max300
import controllers.PSRController
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

class RemoveMemberDetailsController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      withMemberDetails(srn, index)(nameDOB =>
        Ok(view(form(formProvider, nameDOB), viewModel(srn, index, nameDOB, mode)))
      )
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)) match {
        case None =>
          Future.successful(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

        case Some(memberDetails) =>
          form(formProvider, memberDetails)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  withMemberDetails(srn, index)(nameDOB =>
                    BadRequest(view(formWithErrors, viewModel(srn, index, nameDOB, mode)))
                  )
                ),
              removeMemberDetails =>
                if (removeMemberDetails) {
                  for {
                    updatedAnswers <- softDeleteMember(srn, index).mapK[Future]
                    _ <- saveService.save(updatedAnswers)
                    result <- submissionService
                      .submitPsrDetailsWithUA(
                        srn,
                        updatedAnswers,
                        routes.PensionSchemeMembersController.onPageLoad(srn)
                      )
                  } yield result.getOrRecoverJourney(_ =>
                    Redirect(
                      navigator.nextPage(RemoveMemberDetailsPage(srn), mode, updatedAnswers)
                    )
                  )
                } else {
                  Future.successful(
                    Redirect(navigator.nextPage(RemoveMemberDetailsPage(srn), mode, request.userAnswers))
                  )
                }
            )
      }
    }

  private def withMemberDetails(srn: Srn, index: Max300)(
    f: NameDOB => Result
  )(implicit request: DataRequest[?]): Result =
    (
      for {
        nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRedirectToTaskList(srn)
      } yield f(nameDOB)
    ).merge
}
object RemoveMemberDetailsController {
  def form(formProvider: YesNoPageFormProvider, nameDOB: NameDOB): Form[Boolean] = formProvider(
    requiredKey = "removeMemberDetails.error.required",
    args = List(nameDOB.fullName)
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
