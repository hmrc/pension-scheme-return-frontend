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

import config.RefinedTypes.Max5.enumerable
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils.ListOps
import utils.IntUtils.{toInt, toRefined300}
import cats.implicits._
import controllers.actions.IdentifyAndRequireData
import forms.RadioListFormProvider
import config.RefinedTypes.{Max300, Max5}
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.NormalMode
import pages.nonsipp.membertransferout._
import play.api.i18n.MessagesApi
import controllers.nonsipp.membertransferout.WhichTransferOutRemoveController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel}
import models.requests.DataRequest
import play.api.data.Form

import java.time.LocalDate

class WhichTransferOutRemoveController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5] = WhichTransferOutRemoveController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val completed: List[Max5] = request.userAnswers
      .map(MemberTransferOutProgress.all(srn, memberIndex))
      .filter {
        case (_, status) => status.completed
      }
      .keys
      .toList
      .refine[Max5.Refined]

    completed match {
      case Nil =>
        Redirect(
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoad(srn, page = 1, NormalMode)
        )
      case head :: Nil =>
        Redirect(
          controllers.nonsipp.membertransferout.routes.RemoveTransferOutController.onSubmit(srn, memberIndex, head)
        )
      case _ =>
        (
          for {
            memberName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
            values <- getJourneyValues(srn, memberIndex)
          } yield Ok(view(form, viewModel(srn, memberIndex, memberName.fullName, values)))
        ).merge

    }
  }

  def onSubmit(srn: Srn, memberIndex: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors =>
          (
            for {
              memberName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
              values <- getJourneyValues(srn, memberIndex)
            } yield BadRequest(view(errors, viewModel(srn, memberIndex, memberName.fullName, values)))
          ).merge,
        answer =>
          Redirect(
            controllers.nonsipp.membertransferout.routes.RemoveTransferOutController
              .onPageLoad(srn, memberIndex, answer)
          )
      )
  }

  private def getJourneyValues(srn: Srn, memberIndex: Max300)(
    implicit request: DataRequest[_]
  ) =
    request.userAnswers
      .map(MemberTransferOutProgress.all(srn, memberIndex))
      .filter {
        case (_, status) => status.completed
      }
      .keys
      .toList
      .refine[Max5.Refined]
      .traverse { secondaryIndex =>
        for {
          receivingSchemeName <- request.userAnswers
            .get(ReceivingSchemeNamePage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney

          dateOfTransfer <- request.userAnswers
            .get(WhenWasTransferMadePage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney
        } yield (secondaryIndex, receivingSchemeName, dateOfTransfer)
      }

}

object WhichTransferOutRemoveController {
  def form(formProvider: RadioListFormProvider): Form[Max5] =
    formProvider(
      "transferOut.whichTransferOutRemove.error.required"
    )

  private def buildRows(values: List[(Max5, String, LocalDate)]): List[ListRadiosRow] =
    values.flatMap {
      case (index, receivingSchemeName, value) =>
        List(
          ListRadiosRow(
            index.value,
            Message("transferOut.whichTransferOutRemove.radio.label", receivingSchemeName, value.show)
          )
        )
    }.toList

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    values: List[(Max5, String, LocalDate)]
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(values)

    FormPageViewModel(
      title = "transferOut.whichTransferOutRemove.title",
      heading = Message("transferOut.whichTransferOutRemove.heading", memberName),
      description = None,
      page = ListRadiosViewModel(
        legend = None,
        rows = rows
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      routes.WhichTransferOutRemoveController.onSubmit(srn, memberIndex)
    )
  }
}
