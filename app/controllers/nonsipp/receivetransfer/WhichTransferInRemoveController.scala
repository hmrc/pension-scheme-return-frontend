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

import config.RefinedTypes.Max5._
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils.ListOps
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import controllers.nonsipp.receivetransfer.WhichTransferInRemoveController._
import forms.RadioListFormProvider
import models.{Money, NormalMode}
import play.api.i18n.MessagesApi
import config.RefinedTypes._
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import cats.implicits._
import pages.nonsipp.receivetransfer._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel}
import models.requests.DataRequest
import play.api.data.Form

class WhichTransferInRemoveController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5] = WhichTransferInRemoveController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val completed: List[Max5] = request.userAnswers
      .map(ReceiveTransferProgress.all(memberIndex))
      .filter { case (_, status) =>
        status.completed
      }
      .keys
      .toList
      .refine[Max5.Refined]

    completed match {
      case Nil =>
        Redirect(
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, page = 1, NormalMode)
        )
      case head :: Nil =>
        Redirect(
          controllers.nonsipp.receivetransfer.routes.RemoveTransferInController.onSubmit(srn, memberIndex, head)
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
            controllers.nonsipp.receivetransfer.routes.RemoveTransferInController
              .onPageLoad(srn, memberIndex, answer)
          )
      )
  }

  private def getJourneyValues(srn: Srn, memberIndex: Max300)(implicit
    request: DataRequest[?]
  ) =
    request.userAnswers
      .map(ReceiveTransferProgress.all(memberIndex))
      .filter { case (_, status) =>
        status.completed
      }
      .keys
      .toList
      .refine[Max5.Refined]
      .traverse { secondaryIndex =>
        for {
          totalValue <- request.userAnswers
            .get(TotalValueTransferPage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney
          transferringSchemeName <- request.userAnswers
            .get(TransferringSchemeNamePage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney
        } yield (secondaryIndex, totalValue, transferringSchemeName)
      }

}

object WhichTransferInRemoveController {
  def form(formProvider: RadioListFormProvider): Form[Max5] =
    formProvider(
      "whichTransferInRemove.error.required"
    )

  private def buildRows(values: List[(Max5, Money, String)]): List[ListRadiosRow] =
    values.flatMap { case (index, total, transferringSchemeName) =>
      List(
        ListRadiosRow(
          index.value,
          Message("whichTransferInRemove.radio.label", total.displayAs, transferringSchemeName)
        )
      )
    }

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    values: List[(Max5, Money, String)]
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(values)

    FormPageViewModel(
      title = "whichTransferInRemove.title",
      heading = Message("whichTransferInRemove.heading", memberName),
      description = None,
      page = ListRadiosViewModel(
        legend = None,
        rows = rows
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      routes.WhichTransferInRemoveController.onSubmit(srn, memberIndex)
    )
  }
}
