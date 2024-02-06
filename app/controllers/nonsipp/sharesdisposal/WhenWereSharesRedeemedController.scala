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

package controllers.nonsipp.sharesdisposal

import cats.implicits.toShow
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.WhenWereSharesRedeemedController._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.SchemeId.Srn
import models.{DateRange, Mode}
import navigation.Navigator
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.WhenWereSharesRedeemedPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import views.html.DatePageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhenWereSharesRedeemedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: DatePageFormProvider,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: DatePageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form(date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    WhenWereSharesRedeemedController.form(formProvider, date)

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
          val preparedForm = request.userAnswers
            .get(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex))
            .fold(form(date))(form(date).fill)
          Ok(
            view(
              preparedForm,
              viewModel(srn, shareIndex, disposalIndex, companyName, mode)
            )
          )
        }
      }
    }

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
          form(date)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, shareIndex, disposalIndex, companyName, mode)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex), mode, updatedAnswers)
                )
            )
        }
      }
    }
}

object WhenWereSharesRedeemedController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "sharesDisposal.whenWereSharesRedeemed.error.required.all",
        requiredDay = "sharesDisposal.whenWereSharesRedeemed.error.required.day",
        requiredMonth = "sharesDisposal.whenWereSharesRedeemed.error.required.month",
        requiredYear = "sharesDisposal.whenWereSharesRedeemed.error.required.year",
        requiredTwo = "sharesDisposal.whenWereSharesRedeemed.error.required.two",
        invalidDate = "sharesDisposal.whenWereSharesRedeemed.error.invalid.date",
        invalidCharacters = "sharesDisposal.whenWereSharesRedeemed.error.invalid.characters",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("sharesDisposal.whenWereSharesRedeemed.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("sharesDisposal.whenWereSharesRedeemed.error.date.before", date.from.show)
            )
        )
      )
    )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      title = Message("sharesDisposal.whenWereSharesRedeemed.title"),
      heading = Message("sharesDisposal.whenWereSharesRedeemed.heading", companyName),
      page = DatePageViewModel(None, Message("sharesDisposal.whenWereSharesRedeemed.heading", companyName)),
      onSubmit = routes.WhenWereSharesRedeemedController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
