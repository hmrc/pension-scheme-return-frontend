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

package controllers.nonsipp.accountingperiod

import services.SaveService
import viewmodels.implicits._
import play.api.mvc._
import controllers.nonsipp.accountingperiod.RemoveAccountingPeriodController._
import cats.implicits.toShow
import controllers.actions._
import pages.nonsipp.accountingperiod.{AccountingPeriodPage, RemoveAccountingPeriodPage}
import navigation.Navigator
import forms.YesNoPageFormProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import config.RefinedTypes.Max3
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveAccountingPeriodController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = RemoveAccountingPeriodController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      withAccountingPeriodAtIndex(srn, index, mode) { period =>
        Ok(view(form, viewModel(srn, index, period, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              withAccountingPeriodAtIndex(srn, index, mode) { period =>
                BadRequest(view(formWithErrors, viewModel(srn, index, period, mode)))
              }
            ),
          answer => {
            if (answer) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(AccountingPeriodPage(srn, index, mode)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, updatedAnswers))
            } else {
              Future
                .successful(
                  Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, request.userAnswers))
                )
            }
          }
        )
  }

  private def withAccountingPeriodAtIndex(srn: Srn, index: Max3, mode: Mode)(
    f: DateRange => Result
  )(implicit request: DataRequest[_]): Result =
    (
      for {
        bankAccount <- request.userAnswers.get(AccountingPeriodPage(srn, index, mode)).getOrRedirectToTaskList(srn)
      } yield {
        f(bankAccount)
      }
    ).merge

}

object RemoveAccountingPeriodController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeAccountingPeriod.error.required"
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeAccountingPeriod.title", dateRange.from.show, dateRange.to.show),
      Message("removeAccountingPeriod.heading", dateRange.from.show, dateRange.to.show),
      routes.RemoveAccountingPeriodController.onSubmit(srn, index, mode)
    )
}
