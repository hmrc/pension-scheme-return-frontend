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

package controllers.nonsipp.bonds

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.nonsipp.summary.BondsCheckAnswersUtils
import controllers.PSRController
import controllers.actions._
import models._
import play.api.i18n.MessagesApi
import viewmodels.models._
import views.html.PrePopCheckYourAnswersView
import models.SchemeId.Srn

class BondsCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: PrePopCheckYourAnswersView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    BondsCheckAnswersUtils
      .summaryData(srn, index, NormalMode)
      .map { data =>
        val sections = BondsCheckAnswersUtils
          .viewModel(
            data.srn,
            data.index,
            data.schemeName,
            data.nameOfBonds,
            data.whyDoesSchemeHoldBonds,
            data.whenDidSchemeAcquireBonds,
            data.costOfBonds,
            data.bondsFromConnectedParty,
            data.areBondsUnregulated,
            data.incomeFromBonds,
            data.mode,
            data.viewOnlyUpdated,
            data.optYear,
            data.optCurrentVersion,
            data.optPreviousVersion
          )
          .page
          .sections
        Ok(
          view(
            BondsCheckAndUpdateController.viewModel(
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
    Redirect(routes.IncomeFromBondsController.onPageLoad(srn, index, NormalMode))
  }
}

object BondsCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Int,
    sections: List[CheckYourAnswersSection]
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = NormalMode,
      title = "bondsCheckAndUpdate.title",
      heading = "bondsCheckAndUpdate.heading",
      description = Some("bondsCheckAndUpdate.description"),
      page = CheckYourAnswersViewModel(sections),
      refresh = None,
      buttonText = "bondsCheckAndUpdate.button",
      details = None,
      onSubmit = routes.BondsCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
}
