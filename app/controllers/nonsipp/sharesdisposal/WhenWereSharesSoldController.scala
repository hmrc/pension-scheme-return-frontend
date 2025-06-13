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

import viewmodels.implicits._
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.WhenWereSharesSoldPage
import navigation.Navigator
import forms.DatePageFormProvider
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.data.Form
import forms.mappings.errors.DateFormErrors
import services.{SaveService, SchemeDateService}
import controllers.nonsipp.sharesdisposal.WhenWereSharesSoldController._
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

class WhenWereSharesSoldController @Inject() (
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
    WhenWereSharesSoldController.form(formProvider, date)

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
          val preparedForm = request.userAnswers
            .get(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex))
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
                    .fromTry(request.userAnswers.set(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), value))
                  nextPage = navigator
                    .nextPage(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), mode, updatedAnswers)
                  updatedProgressAnswers <- saveProgress(srn, shareIndex, disposalIndex, updatedAnswers, nextPage)
                  _ <- saveService.save(updatedProgressAnswers)
                } yield Redirect(nextPage)
            )
        }
      }
    }
}

object WhenWereSharesSoldController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "sharesDisposal.whenWereSharesSold.error.required.all",
        requiredDay = "sharesDisposal.whenWereSharesSold.error.required.day",
        requiredMonth = "sharesDisposal.whenWereSharesSold.error.required.month",
        requiredYear = "sharesDisposal.whenWereSharesSold.error.required.year",
        requiredTwo = "sharesDisposal.whenWereSharesSold.error.required.two",
        invalidDate = "sharesDisposal.whenWereSharesSold.error.invalid.date",
        invalidCharacters = "sharesDisposal.whenWereSharesSold.error.invalid.characters",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("sharesDisposal.whenWereSharesSold.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("sharesDisposal.whenWereSharesSold.error.date.before", date.from.show)
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
      title = Message("sharesDisposal.whenWereSharesSold.title"),
      heading = Message("sharesDisposal.whenWereSharesSold.heading", companyName),
      page = DatePageViewModel(
        None,
        Message("sharesDisposal.whenWereSharesSold.heading", companyName),
        Some("sharesDisposal.whenWereSharesSold.hint")
      ),
      onSubmit = routes.WhenWereSharesSoldController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
