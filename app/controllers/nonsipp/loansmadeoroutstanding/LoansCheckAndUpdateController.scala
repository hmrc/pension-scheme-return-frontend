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

package controllers.nonsipp.loansmadeoroutstanding

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.common.OtherRecipientDetailsPage
import pages.nonsipp.loansmadeoroutstanding._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ContentTablePageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import controllers.nonsipp.loansmadeoroutstanding.LoansCheckAndUpdateController._
import utils.DateTimeUtils.localDateShow
import models.{IdentitySubject, Money, NormalMode}
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ListMessage, Message, ParagraphMessage}
import viewmodels.models.{ContentTablePageViewModel, FormPageViewModel}

import java.time.LocalDate

class LoansCheckAndUpdateController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentTablePageView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (
      for {
        recipientName <- List(
          request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
          request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
          request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
          request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)).map(_.name)
        ).flatten.headOption.getOrRecoverJourney
        datePeriodDetails <- request.userAnswers.get(DatePeriodLoanPage(srn, index)).getOrRecoverJourney
        amountOfTheLoan <- request.userAnswers.get(AmountOfTheLoanPage(srn, index)).getOrRecoverJourney
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            recipientName,
            datePeriodDetails._1,
            amountOfTheLoan.loanAmount
          )
        )
      )
    ).merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.AmountOfTheLoanController.onPageLoad(srn, index, NormalMode))
  }
}

object LoansCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    recipientName: String,
    dateOfTheLoan: LocalDate,
    amountOfTheLoan: Money
  ): FormPageViewModel[ContentTablePageViewModel] = {

    val rows: List[(DisplayMessage, DisplayMessage)] = List(
      Message("loansCheckAndUpdate.table.one") -> Message(recipientName),
      Message("loansCheckAndUpdate.table.two") -> Message(dateOfTheLoan.show),
      Message("loansCheckAndUpdate.table.three") -> Message(amountOfTheLoan.displayAs)
    )

    FormPageViewModel(
      mode = NormalMode,
      title = "loansCheckAndUpdate.title",
      heading = "loansCheckAndUpdate.heading",
      description = None,
      page = ContentTablePageViewModel(
        inset = None,
        beforeTable = Some(ParagraphMessage("loansCheckAndUpdate.paragraph")),
        afterTable = Some(
          ParagraphMessage("loansCheckAndUpdate.bullet.paragraph") ++ ListMessage
            .Bullet(
              "loansCheckAndUpdate.bullet.one",
              "loansCheckAndUpdate.bullet.two"
            )
        ),
        rows = rows
      ),
      refresh = None,
      buttonText = "loansCheckAndUpdate.button",
      details = None,
      onSubmit = routes.LoansCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
  }
}
