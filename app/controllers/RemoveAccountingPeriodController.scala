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

package controllers

import cats.implicits.toShow
import config.Refined.Max3
import controllers.RemoveAccountingPeriodController._
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import navigation.Navigator
import pages.{AccountingPeriodPage, RemoveAccountingPeriodPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAccountingPeriodController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         navigator: Navigator,
                                         identifyAndRequireData: IdentifyAndRequireData,
                                         formProvider: YesNoPageFormProvider,
                                         saveService: SaveService,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: YesNoPageView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = RemoveSchemeBankAccountController.form(formProvider)

  def onPageLoad(srn:Srn, index: Max3, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request => withAccountingPeriodAtIndex(srn, index) { period =>
      Ok(view(form, viewModel(srn, index, period, mode)))
    }
  }

  def onSubmit(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(
          withAccountingPeriodAtIndex(srn, index) { period =>
            BadRequest(view(formWithErrors, viewModel(srn, index, period, mode)))
          }
        ),
        answer => {
          if(answer){
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(AccountingPeriodPage(srn, index)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn), mode, updatedAnswers))
          } else {
            Future.successful(Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn), mode, request.userAnswers)))
          }
        }
      )
  }

  private def withAccountingPeriodAtIndex(srn: Srn, index: Max3)(f: DateRange => Result)(implicit request: DataRequest[_]): Result =
    request.userAnswers.get(AccountingPeriodPage(srn, index)) match {
      case Some(bankAccount) => f(bankAccount)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

}

object RemoveAccountingPeriodController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeAccountingPeriod.error.required"
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    SimpleMessage("removeAccountingPeriod.title", dateRange.from.show, dateRange.to.show),
    SimpleMessage("removeAccountingPeriod.heading", dateRange.from.show, dateRange.to.show),
    controllers.routes.RemoveAccountingPeriodController.onSubmit(srn, index, mode)
  )
}