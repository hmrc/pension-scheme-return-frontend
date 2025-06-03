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

package controllers.nonsipp.sharesdisposal

import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.WhenWereSharesRedeemedController._
import pages.nonsipp.sharesdisposal.WhenWereSharesRedeemedPage
import navigation.Navigator
import forms.DatePageFormProvider
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.data.Form
import forms.mappings.errors.DateFormErrors
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
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

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
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
                  nextPage = navigator
                    .nextPage(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex), mode, updatedAnswers)
                  updatedProgressAnswers <- saveProgress(srn, shareIndex, disposalIndex, updatedAnswers, nextPage)
                  _ <- saveService.save(updatedProgressAnswers)
                } yield Redirect(nextPage)
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
