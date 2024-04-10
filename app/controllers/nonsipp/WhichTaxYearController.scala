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

package controllers.nonsipp

import services.{SaveService, TaxYearService}
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import cats.implicits.toShow
import controllers.actions._
import forms.RadioListFormProvider
import uk.gov.hmrc.time.TaxYear
import play.api.data.Form
import views.html.RadioListView
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhichTaxYearController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView,
  formProvider: RadioListFormProvider,
  taxYearService: TaxYearService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  implicit val allDates: Enumerable[DateRange] = WhichTaxYearController.options(taxYearService.current)
  val form: Form[DateRange] = WhichTaxYearController.form(formProvider, allDates)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(WhichTaxYearPage(srn)),
          WhichTaxYearController.viewModel(srn, mode, taxYearService.current)
        )
      )
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(view(errors, WhichTaxYearController.viewModel(srn, mode, taxYearService.current)))
            ),
          success =>
            for {
              userAnswers <- Future.fromTry(request.userAnswers.set(WhichTaxYearPage(srn), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(WhichTaxYearPage(srn), NormalMode, userAnswers))
            }
        )
    }
}

object WhichTaxYearController {

  def form(formProvider: RadioListFormProvider, values: Enumerable[DateRange]): Form[DateRange] =
    formProvider[DateRange]("whichTaxYear.error.required")(values)

  def options(startingTaxYear: TaxYear): Enumerable[DateRange] = {

    val taxYears = List.iterate(startingTaxYear, 7)(_.previous)

    val taxYearRanges = taxYears.map(DateRange.from).map(r => (r.toString, r))

    Enumerable(taxYearRanges: _*)
  }

  def viewModel(srn: Srn, mode: Mode, taxYear: TaxYear): FormPageViewModel[RadioListViewModel] = RadioListViewModel(
    "whichTaxYear.title",
    "whichTaxYear.heading",
    options(taxYear).toList.map {
      case (value, range) =>
        val displayText = Message("whichTaxYear.radioOption", range.from.show, range.to.show)
        RadioListRowViewModel(displayText, value)
    },
    routes.WhichTaxYearController.onSubmit(srn, mode)
  )
}
