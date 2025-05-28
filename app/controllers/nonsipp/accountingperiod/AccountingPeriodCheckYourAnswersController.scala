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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import config.RefinedTypes.Max3
import utils.IntUtils.IntOpts
import cats.implicits.toShow
import controllers.actions._
import pages.nonsipp.accountingperiod.{AccountingPeriodCheckYourAnswersPage, AccountingPeriodPage}
import navigation.Navigator
import viewmodels.models._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import controllers.nonsipp.accountingperiod.AccountingPeriodCheckYourAnswersController.viewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Named

class AccountingPeriodCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      request.userAnswers.get(AccountingPeriodPage(srn, index.refined, mode)) match {
        case None =>
          Redirect(routes.AccountingPeriodController.onPageLoad(srn, index, mode))
        case Some(accountingPeriod) =>
          Ok(view(viewModel(srn, index.refined, accountingPeriod, mode)))
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(AccountingPeriodCheckYourAnswersPage(srn, mode), mode, request.userAnswers))
    }
}

object AccountingPeriodCheckYourAnswersController {

  private def rows(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("site.startDate", dateRange.from.show)
      .withAction(
        SummaryAction(
          "site.change",
          routes.AccountingPeriodController.onPageLoad(srn, index.value, mode).url + "#startDate"
        ).withVisuallyHiddenContent("site.startDate")
      ),
    CheckYourAnswersRowViewModel("site.endDate", dateRange.to.show)
      .withAction(
        SummaryAction(
          "site.change",
          routes.AccountingPeriodController.onPageLoad(srn, index.value, mode).url + "#endDate"
        ).withVisuallyHiddenContent("site.endDate")
      )
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): FormPageViewModel[CheckYourAnswersViewModel] =
    CheckYourAnswersViewModel(
      rows(srn, index, dateRange, mode),
      routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn, mode)
    )
}
