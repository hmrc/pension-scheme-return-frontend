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

package controllers.nonsipp.landorpropertydisposal

import cats.implicits.toShow
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.WhenWasPropertySoldController._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.WhenWasPropertySoldPage
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

class WhenWasPropertySoldController @Inject()(
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
    (date: LocalDate, beforeDate: LocalDate, request: DataRequest[AnyContent]) =>
      WhenWasPropertySoldController.form(formProvider)(date, beforeDate, request.messages(messagesApi))

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        val preparedForm = {
          request.userAnswers
            .fillForm(
              WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex),
              form(date.to, date.from, request)
            )
        }
        Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, mode)))
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        form(date.to, date.from, request)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, landOrPropertyIndex, disposalIndex, mode)
                  )
                )
              ),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers.set(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex), value)
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator
                  .nextPage(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
              )
          )
      }
    }
}

object WhenWasPropertySoldController {
  def form(
    formProvider: DatePageFormProvider
  )(date: LocalDate, beforeDate: LocalDate, messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "whenWasPropertySold.dateOfSale.error.required.all",
      requiredDay = "whenWasPropertySold.dateOfSale.error.required.day",
      requiredMonth = "whenWasPropertySold.dateOfSale.error.required.month",
      requiredYear = "whenWasPropertySold.dateOfSale.error.required.year",
      requiredTwo = "whenWasPropertySold.dateOfSale.error.required.two",
      invalidDate = "whenWasPropertySold.dateOfSale.error.invalid.date",
      invalidCharacters = "whenWasPropertySold.dateOfSale.error.invalid.characters",
      validators = List(
        DateFormErrors
          .failIfDateAfter(date, messages("whenWasPropertySold.dateOfSale.error.future", date.show)),
        DateFormErrors
          .failIfDateBefore(beforeDate, messages("whenWasPropertySold.dateOfSale.error.after", beforeDate.show))
      )
    )
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "whenWasPropertySold.title",
      Message("whenWasPropertySold.heading", "12 Holly Lane"),
      DatePageViewModel(),
      routes.WhenWasPropertySoldController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}