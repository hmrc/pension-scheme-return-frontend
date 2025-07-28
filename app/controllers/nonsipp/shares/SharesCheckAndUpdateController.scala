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

package controllers.nonsipp.shares

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.nonsipp.summary.SharesCheckAnswersUtils
import controllers.PSRController
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import models._
import play.api.i18n.MessagesApi
import views.html.PrePopCheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

class SharesCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: PrePopCheckYourAnswersView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    SharesCheckAnswersUtils
      .summaryData(srn, index, NormalMode)
      .map { data =>
        val sections = SharesCheckAnswersUtils
          .viewModel(
            data
          )
          .page
          .sections
        Ok(
          view(
            SharesCheckAndUpdateController.viewModel(
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
    Redirect(routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode))
  }
}

object SharesCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Int,
    sections: List[CheckYourAnswersSection]
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = NormalMode,
      title = "sharesCheckAndUpdate.title",
      heading = "sharesCheckAndUpdate.heading",
      description = Some("sharesCheckAndUpdate.description"),
      page = CheckYourAnswersViewModel(sections),
      refresh = None,
      buttonText = "sharesCheckAndUpdate.button",
      details = None,
      onSubmit = routes.SharesCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
}
