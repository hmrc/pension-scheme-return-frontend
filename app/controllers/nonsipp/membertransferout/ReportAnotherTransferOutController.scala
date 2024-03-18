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

package controllers.nonsipp.membertransferout

import services.SaveService
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import config.Refined.{Max300, Max5}
import controllers.nonsipp.membertransferout.ReportAnotherTransferOutController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import pages.nonsipp.membertransferout.ReportAnotherTransferOutPage
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class ReportAnotherTransferOutController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = ReportAnotherTransferOutController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers.fillForm(ReportAnotherTransferOutPage(srn, index, secondaryIndex), form)
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
                  .set(ReportAnotherTransferOutPage(srn, index, secondaryIndex), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(ReportAnotherTransferOutPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object ReportAnotherTransferOutController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "reportAnotherTransferOut.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    mode: Mode,
    memberName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "reportAnotherTransferOut.title",
      Message("reportAnotherTransferOut.heading", memberName),
      YesNoPageViewModel(
        hint = Some("reportAnotherTransferOut.hint")
      ),
      routes.ReportAnotherTransferOutController.onSubmit(srn, index, secondaryIndex, mode)
    )

}
