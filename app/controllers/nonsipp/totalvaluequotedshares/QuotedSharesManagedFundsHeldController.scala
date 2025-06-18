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

package controllers.nonsipp.totalvaluequotedshares

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import cats.{Id, Monad}
import controllers.nonsipp.totalvaluequotedshares.QuotedSharesManagedFundsHeldController.viewModel
import pages.nonsipp.totalvaluequotedshares.{QuotedSharesManagedFundsHeldPage, TotalValueQuotedSharesPage}
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode, Money}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class QuotedSharesManagedFundsHeldController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSchemeDate[Id](srn) { period =>
      val form = QuotedSharesManagedFundsHeldController.form(formProvider, period)
      val preparedForm = request.userAnswers.get(QuotedSharesManagedFundsHeldPage(srn)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode, period)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSchemeDate(srn) { period =>
      val form = QuotedSharesManagedFundsHeldController.form(formProvider, period)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode, period)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry {
                val baseUpdate = request.userAnswers.set(QuotedSharesManagedFundsHeldPage(srn), value)
                val finalUpdate = if (!value) {
                  baseUpdate.flatMap { updatedUserAnswers =>
                    updatedUserAnswers.set(TotalValueQuotedSharesPage(srn), Money(0.00))
                  }
                } else {
                  baseUpdate
                }
                finalUpdate
              }
              _ <- saveService.save(updatedAnswers)
              redirectTo <-
                if (value) {
                  Future.successful(
                    Redirect(navigator.nextPage(QuotedSharesManagedFundsHeldPage(srn), mode, updatedAnswers))
                  )
                } else {
                  psrSubmissionService
                    .submitPsrDetailsWithUA(
                      srn,
                      updatedAnswers,
                      fallbackCall =
                        controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController.onPageLoad(srn, mode)
                    )(using implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                    .map {
                      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                      case Some(_) =>
                        Redirect(navigator.nextPage(QuotedSharesManagedFundsHeldPage(srn), mode, updatedAnswers))
                    }
                }
            } yield redirectTo
        )
    }
  }

  private def usingSchemeDate[F[_]: Monad](
    srn: Srn
  )(body: DateRange => F[Result])(implicit request: DataRequest[?]): F[Result] =
    schemeDateService.schemeDate(srn) match {
      case Some(period) => body(period)
      case None => Monad[F].pure(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}
object QuotedSharesManagedFundsHeldController {
  def form(formProvider: YesNoPageFormProvider, period: DateRange): Form[Boolean] = formProvider(
    requiredKey = "quotedSharesManagedFundsHeld.error.required",
    args = List(period.to.show)
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode, period: DateRange): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("quotedSharesManagedFundsHeld.title", period.to.show),
      Message("quotedSharesManagedFundsHeld.heading", schemeName, period.to.show),
      YesNoPageViewModel(
        hint = Some(Message("quotedSharesManagedFundsHeld.hint"))
      ),
      routes.QuotedSharesManagedFundsHeldController.onSubmit(srn, mode)
    )
}
