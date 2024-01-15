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

package controllers.nonsipp.shares

import cats.implicits.toShow
import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.WhenDidSchemeAcquireSharesController._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.shares.{TypeOfSharesHeldPage, WhenDidSchemeAcquireSharesPage}
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
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhenDidSchemeAcquireSharesController @Inject()(
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
      WhenDidSchemeAcquireSharesController.form(formProvider)(date, request.messages(messagesApi))

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
          val preparedForm = {
            request.userAnswers.fillForm(WhenDidSchemeAcquireSharesPage(srn, index), form(date.to, request))
          }
          Ok(view(preparedForm, viewModel(srn, index, mode, request.schemeDetails.schemeName, typeOfShares.name)))
        }
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
          form(date.to, request)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, index, mode, request.schemeDetails.schemeName, typeOfShares.name)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(WhenDidSchemeAcquireSharesPage(srn, index), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(WhenDidSchemeAcquireSharesPage(srn, index), mode, updatedAnswers)
                )
            )
        }
      }
  }
}

object WhenDidSchemeAcquireSharesController {
  def form(formProvider: DatePageFormProvider)(date: LocalDate, messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "whenDidSchemeAcquireShares.dateOfAcquired.error.required.all",
      requiredDay = "whenDidSchemeAcquireShares.dateOfAcquired.error.required.day",
      requiredMonth = "whenDidSchemeAcquireShares.dateOfAcquired.error.required.month",
      requiredYear = "whenDidSchemeAcquireShares.dateOfAcquired.error.required.year",
      requiredTwo = "whenDidSchemeAcquireShares.dateOfAcquired.error.required.two",
      invalidDate = "whenDidSchemeAcquireShares.dateOfAcquired.error.invalid.date",
      invalidCharacters = "whenDidSchemeAcquireShares.dateOfAcquired.error.invalid.characters",
      validators = List(
        DateFormErrors
          .failIfDateAfter(date, messages("whenDidSchemeAcquireShares.dateOfAcquired.error.future", date.show)),
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            messages(
              "whenDidSchemeAcquireShares.dateOfAcquired.error.after",
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
    typeOfShares: String
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "whenDidSchemeAcquireShares.title",
      Message(
        "whenDidSchemeAcquireShares.heading",
        schemeName,
        Message(s"whyDoesSchemeHoldShares.heading.type.$typeOfShares")
      ),
      DatePageViewModel(None, Message("whenDidSchemeAcquireShares.heading", schemeName, typeOfShares)),
      routes.WhenDidSchemeAcquireSharesController.onSubmit(srn, index, mode)
    )
}
