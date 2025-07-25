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

package controllers.nonsipp.loansmadeoroutstanding

import services.SchemeDateService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import utils.nonsipp.summary.LoansCheckAnswersUtils
import utils.IntUtils.toInt
import utils.IntUtils.given
import controllers.actions.IdentifyAndRequireData
import models.NormalMode
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.PrePopCheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersSection, CheckYourAnswersViewModel, FormPageViewModel}

class LoansCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: PrePopCheckYourAnswersView
) extends PSRController {
  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    LoansCheckAnswersUtils(schemeDateService)
      .summaryData(srn, index, NormalMode)
      .map { data =>
        val sections = LoansCheckAnswersUtils(schemeDateService)
          .viewModel(
            data
          )
          .page
          .sections
        Ok(
          view(
            LoansCheckAndUpdateController.viewModel(
              data.srn,
              data.index,
              sections
            )
          )
        )
      }
      .merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.AmountOfTheLoanController.onPageLoad(srn, index, NormalMode))
  }
}

object LoansCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    sections: List[CheckYourAnswersSection]
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = NormalMode,
      title = "loansCheckAndUpdate.title",
      heading = "loansCheckAndUpdate.heading",
      description = Some("loansCheckAndUpdate.description"),
      page = CheckYourAnswersViewModel(sections),
      refresh = None,
      buttonText = "loansCheckAndUpdate.button",
      details = None,
      onSubmit = routes.LoansCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
}
