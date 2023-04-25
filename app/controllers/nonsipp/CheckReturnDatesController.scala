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

package controllers.nonsipp

import cats.implicits.toShow
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, MinimalSchemeDetails, Mode, PensionSchemeId}
import navigation.Navigator
import pages.nonsipp._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDetailsService, TaxYearService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnDatesController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  taxYear: TaxYearService,
  schemeDetailsService: SchemeDetailsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = CheckReturnDatesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      getMinimalSchemeDetails(request.pensionSchemeId, srn) { details =>
        val preparedForm = request.userAnswers.fillForm(CheckReturnDatesPage(srn), form)

        getWhichTaxYear(srn) { taxYear =>
          val viewModel = CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to, details)
          Future.successful(Ok(view(preparedForm, viewModel)))
        }
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      getMinimalSchemeDetails(request.pensionSchemeId, srn) { details =>
        getWhichTaxYear(srn) { taxYear =>
          val viewModel =
            CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to, details)

          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckReturnDatesPage(srn), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(navigator.nextPage(CheckReturnDatesPage(srn), mode, updatedAnswers))
            )
        }
      }
    }

  private def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    f: MinimalSchemeDetails => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    schemeDetailsService.getMinimalSchemeDetails(id, srn).flatMap {
      case Some(schemeDetails) => f(schemeDetails)
      case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
    }

  private def getWhichTaxYear(
    srn: Srn
  )(f: DateRange => Future[Result])(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(taxYear) => f(taxYear)
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}

object CheckReturnDatesController {

  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("checkReturnDates.error.required", "checkReturnDates.error.invalid")

  private def max(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d1 else d2

  private def min(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d2 else d1

  def viewModel(
    srn: Srn,
    mode: Mode,
    fromDate: LocalDate,
    toDate: LocalDate,
    schemeDetails: MinimalSchemeDetails
  ): YesNoPageViewModel =
    YesNoPageViewModel(
      Message("checkReturnDates.title"),
      Message("checkReturnDates.heading"),
      List(
        ParagraphMessage(
          Message(
            "checkReturnDates.description",
            max(schemeDetails.openDate.getOrElse(fromDate), fromDate).show,
            min(schemeDetails.windUpDate.getOrElse(toDate), toDate).show
          )
        )
      ),
      Some(Message("checkReturnDates.legend")),
      None,
      onSubmit = routes.CheckReturnDatesController.onSubmit(srn, mode)
    )
}
