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

package controllers.nonsipp.moneyborrowed

import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Constants
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.DatePageFormProvider
import models.Mode
import forms.mappings.errors.DateFormErrors
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import pages.nonsipp.moneyborrowed.{BorrowedAmountAndRatePage, LenderNamePage, WhenBorrowedPage}
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LenderNamePage(srn, index.refined)).getOrRecoverJourney { lenderName =>
          request.userAnswers.get(BorrowedAmountAndRatePage(srn, index.refined)).getOrRecoverJourney { amountBorrowed =>
            val preparedForm = {
              request.userAnswers.fillForm(WhenBorrowedPage(srn, index.refined), form(date.to, request))
            }
            Ok(
              view(
                preparedForm,
                WhenBorrowedController
                  .viewModel(
                    srn,
                    index.refined,
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

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LenderNamePage(srn, index.refined)).getOrRecoverJourney { lenderName =>
          request.userAnswers.get(BorrowedAmountAndRatePage(srn, index.refined)).getOrRecoverJourney { amountBorrowed =>
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
                          index.refined,
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
                    updatedAnswers <- request.userAnswers.set(WhenBorrowedPage(srn, index.refined), value).mapK[Future]
                    nextPage = navigator.nextPage(WhenBorrowedPage(srn, index.refined), mode, updatedAnswers)
                    updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
                    _ <- saveService.save(updatedProgressAnswers)
                  } yield Redirect(
                    nextPage
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
      DatePageViewModel(
        None,
        Message("moneyBorrowed.WhenBorrowed.heading", schemeName, lenderName, amountBorrowed),
        Some("moneyBorrowed.WhenBorrowed.hint")
      ),
      controllers.nonsipp.moneyborrowed.routes.WhenBorrowedController.onSubmit(srn, index, mode)
    )
}
