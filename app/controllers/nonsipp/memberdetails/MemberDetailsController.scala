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

import services.{SaveService, SchemeDateService}
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max300
import config.Constants
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import navigation.Navigator
import forms.NameDOBFormProvider
import models.{Mode, NameDOB}
import forms.mappings.errors.DateFormErrors
import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.nonsipp.memberdetails.MemberDetailsController._
import views.html.NameDOBView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, NameDOBViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}

class MemberDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: NameDOBFormProvider,
  view: NameDOBView,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val form = MemberDetailsController.form(formProvider, getTaxDates(srn)(using request))

      Ok(view(form.fromUserAnswers(MemberDetailsPage(srn, index)), viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val form = MemberDetailsController.form(formProvider, getTaxDates(srn)(using request))
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, index, mode)))
            ),
          value =>
            for {
              updatedAnswers <- request.userAnswers.set(MemberDetailsPage(srn, index), value).mapK
              nextPage = navigator.nextPage(MemberDetailsPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }

  private def getTaxDates(srn: Srn)(implicit request: DataRequest[AnyContent]): Option[LocalDate] =
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(taxPeriod) =>
        taxPeriod.fold(l => Some(l.to), r => Some(r.map(x => x._1).toList.sortBy(_.to).reverse.head.to))
      case _ => None
    }

}

object MemberDetailsController {

  def form(
    formProvider: NameDOBFormProvider,
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Form[NameDOB] = {
    val dateThreshold: LocalDate = validDateThreshold.getOrElse(LocalDate.now())
    formProvider(
      "memberDetails.firstName.error.required",
      "memberDetails.firstName.error.invalid",
      "memberDetails.firstName.error.length",
      "memberDetails.lastName.error.required",
      "memberDetails.lastName.error.invalid",
      "memberDetails.lastName.error.length",
      DateFormErrors(
        "memberDetails.dateOfBirth.error.required.all",
        "memberDetails.dateOfBirth.error.required.day",
        "memberDetails.dateOfBirth.error.required.month",
        "memberDetails.dateOfBirth.error.required.year",
        "memberDetails.dateOfBirth.error.required.two",
        "memberDetails.dateOfBirth.error.invalid.date",
        "memberDetails.dateOfBirth.error.invalid.characters",
        List(
          DateFormErrors
            .failIfDateAfter(
              dateThreshold,
              messages(
                "memberDetails.dateOfBirth.error.future",
                dateThreshold.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
              )
            ),
          DateFormErrors
            .failIfDateBefore(
              Constants.earliestDate,
              messages(
                "memberDetails.dateOfBirth.error.after",
                Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
              )
            )
        )
      )
    )
  }

  def viewModel(srn: Srn, index: Max300, mode: Mode): FormPageViewModel[NameDOBViewModel] = FormPageViewModel(
    Message("memberDetails.title"),
    Message("memberDetails.heading"),
    NameDOBViewModel(
      Message("memberDetails.firstName"),
      Message("memberDetails.lastName"),
      Message("memberDetails.dateOfBirth"),
      Message("memberDetails.dateOfBirth.hint")
    ),
    routes.MemberDetailsController.onSubmit(srn, index, mode)
  )
}
