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

package controllers.nonsipp.moneyborrowed

import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.moneyborrowed.{BorrowedAmountAndRatePage, LenderNamePage, WhenBorrowedPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import views.html.DatePageView
import viewmodels.implicits._

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhenBorrowedController @Inject()(
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

  private val form =
    (date: LocalDate, request: DataRequest[AnyContent]) =>
      WhenBorrowedController.form(formProvider)(date, request.messages(messagesApi))

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney { lenderName =>
          request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney { amountBorrowed =>
            val preparedForm = {
              request.userAnswers.fillForm(WhenBorrowedPage(srn, index), form(date.to, request))
            }
            Ok(
              view(
                preparedForm,
                WhenBorrowedController
                  .viewModel(
                    srn,
                    index,
                    mode,
                    request.schemeDetails.schemeName,
                    amountBorrowed._1.displayAs,
                    lenderName
                  )
              )
            )
          }
        }
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney { lenderName =>
          request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney { amountBorrowed =>
            form(date.to, request)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        WhenBorrowedController.viewModel(
                          srn,
                          index,
                          mode,
                          request.schemeDetails.schemeName,
                          amountBorrowed._1.displayAs,
                          lenderName
                        )
                      )
                    )
                  ),
                value =>
                  for {
                    updatedAnswers <- Future
                      .fromTry(request.userAnswers.set(WhenBorrowedPage(srn, index), value))
                    _ <- saveService.save(updatedAnswers)
                  } yield Redirect(
                    navigator.nextPage(WhenBorrowedPage(srn, index), mode, updatedAnswers)
                  )
              )
          }
        }
      }
  }
}

object WhenBorrowedController {
  def form(formProvider: DatePageFormProvider)(date: LocalDate, messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "moneyBorrowed.WhenBorrowed.error.required.all",
      requiredDay = "moneyBorrowed.WhenBorrowed.error.required.day",
      requiredMonth = "moneyBorrowed.WhenBorrowed.error.required.month",
      requiredYear = "moneyBorrowed.WhenBorrowed.error.required.year",
      requiredTwo = "moneyBorrowed.WhenBorrowed.error.required.two",
      invalidDate = "moneyBorrowed.WhenBorrowed.error.invalid.date",
      invalidCharacters = "moneyBorrowed.WhenBorrowed.error.invalid.characters",
      validators = List(
        DateFormErrors
          .failIfDateAfter(
            date,
            messages(
              "moneyBorrowed.WhenBorrowed.error.future",
              date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            )
          ),
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            messages(
              "moneyBorrowed.WhenBorrowed.error.after",
              Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            )
          )
      )
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    amountBorrowed: String,
    lenderName: String
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "moneyBorrowed.WhenBorrowed.title",
      Message("moneyBorrowed.WhenBorrowed.heading", schemeName, lenderName, amountBorrowed),
      DatePageViewModel(None, Message("moneyBorrowed.WhenBorrowed.heading", schemeName, lenderName, amountBorrowed)),
      controllers.nonsipp.moneyborrowed.routes.WhenBorrowedController.onSubmit(srn, index, mode)
    )
}
