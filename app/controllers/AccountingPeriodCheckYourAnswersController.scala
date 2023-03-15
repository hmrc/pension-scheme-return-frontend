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
import com.google.inject.Inject
import config.Refined.Max3
import controllers.AccountingPeriodCheckYourAnswersController.viewModel
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{DateRange, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.{AccountingPeriodCheckYourAnswersPage, AccountingPeriodPage, SchemeBankAccountCheckYourAnswersPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersViewModel, SummaryAction}
import views.html.CheckYourAnswersView

class AccountingPeriodCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max3): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      request.userAnswers.get(AccountingPeriodPage(srn, index)) match {
        case None =>
          Redirect(routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode))
        case Some(accountingPeriod) =>
          Ok(view(viewModel(srn, index, accountingPeriod)))
      }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(AccountingPeriodCheckYourAnswersPage(srn), NormalMode, request.userAnswers))
    }
}

object AccountingPeriodCheckYourAnswersController {

  private def rows(srn: Srn, index: Max3, dateRange: DateRange): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("site.startDate", dateRange.from.show)
      .withAction(
        SummaryAction("site.change", routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode).url)
          .withVisuallyHiddenContent("site.startDate")
      ),
    CheckYourAnswersRowViewModel("site.endDate", dateRange.to.show)
      .withAction(
        SummaryAction("site.change", routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode).url)
          .withVisuallyHiddenContent("site.endDate")
      )
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange): CheckYourAnswersViewModel = CheckYourAnswersViewModel(
    rows(srn, index, dateRange),
    controllers.routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn)
  )
}
