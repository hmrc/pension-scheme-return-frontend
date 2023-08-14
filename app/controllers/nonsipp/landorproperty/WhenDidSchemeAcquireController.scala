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

package controllers.nonsipp.landorproperty

import cats.implicits.toShow
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorproperty.WhenDidSchemeAcquireController._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, LandOrPropertyWhenDidSchemeAcquirePage}
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

class WhenDidSchemeAcquireController @Inject()(
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
      WhenDidSchemeAcquireController.form(formProvider)(date, request.messages(messagesApi))

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
          val preparedForm = {
            request.userAnswers.fillForm(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), form(date.to, request))
          }
          Ok(view(preparedForm, viewModel(srn, index, mode, request.schemeDetails.schemeName, address.addressLine1)))
        }
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
          form(date.to, request)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, index, mode, request.schemeDetails.schemeName, address.addressLine1)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), mode, updatedAnswers)
                )
            )
        }
      }
  }
}

object WhenDidSchemeAcquireController {
  def form(formProvider: DatePageFormProvider)(date: LocalDate, messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.required.all",
      requiredDay = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.required.day",
      requiredMonth = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.required.month",
      requiredYear = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.required.year",
      requiredTwo = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.required.two",
      invalidDate = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.invalid.date",
      invalidCharacters = "whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.invalid.characters",
      validators = List(
        DateFormErrors
          .failIfDateAfter(date, messages("whenDidSchemeAcquireLandOrProperty.dateOfAcquired.error.future", date.show))
      )
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    propertyAddress: String
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "whenDidSchemeAcquireLandOrProperty.title",
      Message("whenDidSchemeAcquireLandOrProperty.heading", schemeName, propertyAddress),
      DatePageViewModel(),
      routes.WhenDidSchemeAcquireController.onSubmit(srn, index, mode)
    )
}
