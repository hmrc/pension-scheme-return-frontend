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
import controllers.actions.{AllowAccessActionProvider, DataCreationAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{DateRange, Enumerable, NormalMode}
import navigation.Navigator
import pages.WhichTaxYearPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils.localDateShow
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.{RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichTaxYearController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        allowAccess: AllowAccessActionProvider,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: RadioListView,
                                        formProvider: RadioListFormProvider,
                                        taxYearService: TaxYearService,
                                        saveService: SaveService
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  implicit val allDates = WhichTaxYearController.options(taxYearService.current)
  private val form = formProvider[DateRange]("whichTaxYear.error.required")

  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(WhichTaxYearPage(srn)),
          WhichTaxYearController.viewModel(srn, taxYearService.current)
        )
      )
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest()
        .fold(
          errors  =>
            Future.successful(BadRequest(view(errors, WhichTaxYearController.viewModel(srn, taxYearService.current)))),
          success =>
            for {
              userAnswers <- Future.fromTry(request.userAnswers.set(WhichTaxYearPage(srn), success))
              _           <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(WhichTaxYearPage(srn), NormalMode, userAnswers))
            }
        )
  }
}

object WhichTaxYearController {

  def options(startingTaxYear: TaxYear): Enumerable[DateRange] = {
    val startingDate = DateRange.from(startingTaxYear)

    Enumerable((startingDate.toString, startingDate))
  }

  def viewModel(srn: Srn, taxYear: TaxYear): RadioListViewModel = RadioListViewModel(
    "whichTaxYear.title",
    "whichTaxYear.heading",
    options(taxYear).toList.map { case (value, range) =>
      val displayText = SimpleMessage("whichTaxYear.radioOption", range.from.show, range.to.show)
      RadioListRowViewModel(displayText, value)
    },
    controllers.routes.WhichTaxYearController.onSubmit(srn)
  )
}
