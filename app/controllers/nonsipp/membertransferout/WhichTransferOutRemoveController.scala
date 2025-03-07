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
import cats.implicits._
import forms.RadioListFormProvider
import utils.DateTimeUtils.localDateShow
import play.api.data.Form
import config.RefinedTypes.{Max300, Max5, OneTo5}
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePages, WhenWasTransferMadePages}
import play.api.i18n.MessagesApi
import controllers.nonsipp.membertransferout.WhichTransferOutRemoveController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel}

import scala.collection.immutable.SortedMap

import java.time.LocalDate

class WhichTransferOutRemoveController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5] = WhichTransferOutRemoveController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val dateOfTransfer = request.userAnswers.map(WhenWasTransferMadePages(srn, memberIndex))
    val receivingSchemeName = request.userAnswers.map(ReceivingSchemeNamePages(srn, memberIndex))
    if (dateOfTransfer.size == 1) {
      refineV[OneTo5](dateOfTransfer.head._1.toInt + 1).fold(
        _ => Redirect(controllers.routes.UnauthorisedController.onPageLoad()),
        index =>
          Redirect(
            controllers.nonsipp.membertransferout.routes.RemoveTransferOutController.onSubmit(srn, memberIndex, index)
          )
      )
    } else {
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        withIndexedValues(receivingSchemeName, dateOfTransfer) { sortedValues =>
          Ok(view(form, viewModel(srn, memberIndex, memberName.fullName, sortedValues)))
        }
      }
    }
  }

  def onSubmit(srn: Srn, memberIndex: Max300): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val dateOfTransfer = request.userAnswers.map(WhenWasTransferMadePages(srn, memberIndex))
    val receivingSchemeName = request.userAnswers.map(ReceivingSchemeNamePages(srn, memberIndex))
    form
      .bindFromRequest()
      .fold(
        errors =>
          request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
            withIndexedValues(receivingSchemeName, dateOfTransfer)(
              sortedValues =>
                BadRequest(
                  view(
                    errors,
                    viewModel(srn, memberIndex, memberName.fullName, sortedValues)
                  )
                )
            )
          },
        answer =>
          Redirect(
            controllers.nonsipp.membertransferout.routes.RemoveTransferOutController
              .onPageLoad(srn, memberIndex, answer)
          )
      )
  }

  private def withIndexedValues(receivingSchemeName: Map[String, String], dateOfTransfer: Map[String, LocalDate])(
    f: Map[Int, (String, LocalDate)] => Result
  ): Result = {
    val values = (receivingSchemeName, dateOfTransfer).tupled
    val maybeIndexedValues: Option[List[(Int, (String, LocalDate))]] = values.toList
      .traverse { case (key, value) => key.toIntOption.map(_ -> value) }

    maybeIndexedValues match {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(indexedValues) =>
        val sortedMap = SortedMap.from(indexedValues)
        f(sortedMap)
    }
  }
}

object WhichTransferOutRemoveController {
  def form(formProvider: RadioListFormProvider): Form[Max5] =
    formProvider(
      "transferOut.whichTransferOutRemove.error.required"
    )

  private def buildRows(values: Map[Int, (String, LocalDate)]): List[ListRadiosRow] =
    values.flatMap {
      case (index, value) =>
        refineV[Max5.Refined](index + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRadiosRow(
                index.value,
                Message("transferOut.whichTransferOutRemove.radio.label", value._1, value._2.show)
              )
            )
        )
    }.toList

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    values: Map[Int, (String, LocalDate)]
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
