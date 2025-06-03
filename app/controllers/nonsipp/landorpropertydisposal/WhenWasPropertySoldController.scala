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

package controllers.nonsipp.landorpropertydisposal

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import cats.implicits.toShow
import pages.nonsipp.landorpropertydisposal.WhenWasPropertySoldPage
import controllers.actions._
import navigation.Navigator
import forms.DatePageFormProvider
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import forms.mappings.errors.DateFormErrors
import services.{SaveService, SchemeDateService}
import controllers.nonsipp.landorpropertydisposal.WhenWasPropertySoldController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import utils.DateTimeUtils.localDateShow
import models.Mode
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
          address =>
            val preparedForm = {
              request.userAnswers
                .fillForm(
                  WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex),
                  form(date.to, date.from, request)
                )
            }
            Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)))
        }
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        form(date.to, date.from, request)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
                address =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
                      )
                    )
                  )
              },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers.set(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex), value)
                  )
                nextPage = navigator
                  .nextPage(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(
                  srn,
                  landOrPropertyIndex,
                  disposalIndex,
                  updatedAnswers,
                  nextPage
                )
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
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
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "whenWasPropertySold.title",
      Message("whenWasPropertySold.heading", addressLine1),
      DatePageViewModel(
        None,
        Message("whenWasPropertySold.heading", addressLine1),
        Some(Message("whenWasPropertySold.hint"))
      ),
      routes.WhenWasPropertySoldController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
