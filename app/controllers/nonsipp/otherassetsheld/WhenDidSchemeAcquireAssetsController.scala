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

package controllers.nonsipp.otherassetsheld

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.WhenDidSchemeAcquireAssetsPage
import config.Constants
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import forms.DatePageFormProvider
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import forms.mappings.errors.DateFormErrors
import services.{SaveService, SchemeDateService}
import controllers.nonsipp.otherassetsheld.WhenDidSchemeAcquireAssetsController._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.Mode
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}

class WhenDidSchemeAcquireAssetsController @Inject()(
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
      WhenDidSchemeAcquireAssetsController.form(formProvider)(date, request.messages(messagesApi))

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        val preparedForm = {
          request.userAnswers.fillForm(WhenDidSchemeAcquireAssetsPage(srn, index), form(date.to, request))
        }
        Ok(view(preparedForm, viewModel(srn, index, mode, request.schemeDetails.schemeName)))

      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        form(date.to, request)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index, mode, request.schemeDetails.schemeName)
                  )
                )
              ),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(WhenDidSchemeAcquireAssetsPage(srn, index), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(WhenDidSchemeAcquireAssetsPage(srn, index), mode, updatedAnswers)
              )
          )

      }
  }
}

object WhenDidSchemeAcquireAssetsController {
  def form(formProvider: DatePageFormProvider)(date: LocalDate, messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "otherAssets.whenDidSchemeAcquireAssets.error.required.all",
      requiredDay = "otherAssets.whenDidSchemeAcquireAssets.error.required.day",
      requiredMonth = "otherAssets.whenDidSchemeAcquireAssets.error.required.month",
      requiredYear = "otherAssets.whenDidSchemeAcquireAssets.error.required.year",
      requiredTwo = "otherAssets.whenDidSchemeAcquireAssets.error.required.two",
      invalidDate = "otherAssets.whenDidSchemeAcquireAssets.error.invalid.date",
      invalidCharacters = "otherAssets.whenDidSchemeAcquireAssets.error.invalid.characters",
      validators = List(
        DateFormErrors
          .failIfDateAfter(date, messages("otherAssets.whenDidSchemeAcquireAssets.error.future", date.show)),
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            messages(
              "otherAssets.whenDidSchemeAcquireAssets.error.after",
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
    schemeName: String
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "otherAssets.whenDidSchemeAcquireAssets.title",
      Message(
        "otherAssets.whenDidSchemeAcquireAssets.heading",
        schemeName
      ),
      DatePageViewModel(
        None,
        Message("otherAssets.whenDidSchemeAcquireAssets.heading", schemeName),
        Some("otherAssets.whenDidSchemeAcquireAssets.hint")
      ),
      routes.WhenDidSchemeAcquireAssetsController.onSubmit(srn, index, mode)
    )
}
