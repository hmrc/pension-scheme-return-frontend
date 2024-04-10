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

import services.SaveService
import viewmodels.implicits._
import play.api.mvc._
import cats.implicits.toShow
import controllers.actions._
import forms.YesNoPageFormProvider
import views.html.YesNoPageView
import models.SchemeId.Srn
import pages.nonsipp._
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

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
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = CheckReturnDatesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      val preparedForm = request.userAnswers.fillForm(CheckReturnDatesPage(srn), form)

      getWhichTaxYear(srn) { taxYear =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to)
        Future.successful(Ok(view(preparedForm, viewModel)))
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      getWhichTaxYear(srn) { taxYear =>
        val viewModel =
          CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to)

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

  def viewModel(
    srn: Srn,
    mode: Mode,
    fromDate: LocalDate,
    toDate: LocalDate
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("checkReturnDates.title"),
      Message("checkReturnDates.heading"),
      YesNoPageViewModel(
        legend = Some(Message("checkReturnDates.legend"))
      ),
      onSubmit = routes.CheckReturnDatesController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage(
        Message(
          "checkReturnDates.description",
          fromDate.show,
          toDate.show
        )
      )
    )
}
