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

package controllers.nonsipp.loansmadeoroutstanding

import cats.implicits._
import com.google.inject.Inject
import config.Constants
import config.Constants.maxLoans
import config.Refined.{Max5000, OneTo5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{IdentitySubject, IdentityType, Mode, Money, NormalMode, Pagination}
import navigation.Navigator
import pages.nonsipp.accountingperiod.AccountingPeriodListPage
import pages.nonsipp.common.{IdentityTypes, OtherRecipientDetailsPage}
import pages.nonsipp.loansmadeoroutstanding._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
import views.html.ListView

import javax.inject.Named

class LoansListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = LoansListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      loanRecipients(srn)
        .map(
          recipients =>
            if (recipients.isEmpty) {
              Redirect(routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode))
            } else {
              Ok(view(form, viewModel(srn, page, mode, recipients)))
            }
        )
        .merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    loanRecipients(srn).map { recipients =>
      if (recipients.length == maxLoans) {

        Redirect(navigator.nextPage(AccountingPeriodListPage(srn, addPeriod = false, mode), mode, request.userAnswers))
      } else {

        val viewModel = LoansListController.viewModel(srn, page, mode, recipients)

        form
          .bindFromRequest()
          .fold(
            errors => BadRequest(view(errors, viewModel)),
            answer => Redirect(navigator.nextPage(LoansListPage(srn, answer), mode, request.userAnswers))
          )
      }
    }.merge
  }

  private def loanRecipients(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Refined[Int, OneTo5000], String, Money)]] = {
    val whoReceivedLoans = request.userAnswers
      .map(IdentityTypes(srn, IdentitySubject.LoanRecipient))
      .map {
        case (key, value) =>
          key.toIntOption.flatMap(k => refineV[OneTo5000](k + 1).toOption.map(_ -> value))
      }
      .toList

    for {
      receivedLoans <- whoReceivedLoans.traverse(_.getOrRecoverJourney)
      recipientNames <- receivedLoans.traverse {
        case (index, IdentityType.Individual) =>
          request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.UKCompany) =>
          request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.UKPartnership) =>
          request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.Other) =>
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
            .map(_.name)
            .getOrRecoverJourney
            .map(index -> _)
      }
      recipientDetails <- recipientNames.traverse {
        case (index, recipientName) =>
          request.userAnswers
            .get(AmountOfTheLoanPage(srn, index))
            .map(_._1)
            .getOrRecoverJourney
            .map((index, recipientName, _))
      }
    } yield recipientDetails
  }
}

object LoansListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "loansList.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, recipients: List[(Max5000, String, Money)]): List[ListRow] =
    recipients.flatMap {
      case (index, recipientName, totalLoan) =>
        List(
          ListRow(
            Message("loansList.row", totalLoan.displayAs, recipientName),
            changeUrl = routes.LoansCYAController.onPageLoad(srn, index, Change).url,
            changeHiddenText = Message("loansList.row.change.hidden", totalLoan.displayAs, recipientName),
            removeUrl = routes.RemoveLoanController.onPageLoad(srn, index, mode).url,
            removeHiddenText = Message("loansList.row.remove.hidden", totalLoan.displayAs, recipientName)
          )
        )
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    recipients: List[(Max5000, String, Money)]
  ): FormPageViewModel[ListViewModel] = {

    val title = if (recipients.length == 1) "loansList.title" else "loansList.title.plural"
    val heading = if (recipients.length == 1) "loansList.heading" else "loansList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.loanPageSize,
      recipients.size,
      routes.LoansListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, recipients.length),
      heading = Message(heading, recipients.length),
      description = Some(ParagraphMessage("loansList.description")),
      page = ListViewModel(
        inset = "loansList.inset",
        rows(srn, mode, recipients),
        Message("loansList.radios"),
        showRadios = recipients.length < Constants.maxLoans,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "loansList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit = routes.LoansListController.onSubmit(srn, page, mode)
    )
  }
}
