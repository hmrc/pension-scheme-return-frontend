/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.nonsipp.declaration

import services.SchemeDateService
import play.api.mvc._
import utils.nonsipp.summary._
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import models.NormalMode
import play.api.i18n._
import views.html.SummaryView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SummaryPageEntry, _}

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class PreSubmissionSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val loansCheckAnswersUtils = LoansCheckAnswersUtils(schemeDateService)

    (for {
      allLandOrPropertyEntries <- LandOrPropertyCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allBondsEntries <- BondsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allSharesEntries <- SharesCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allLoansEntries <- loansCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allOtherAssetsEntries <- OtherAssetsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allMoneyBorrowedEntries <- MoneyBorrowedCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
      allEntries =
        allLandOrPropertyEntries ++ allBondsEntries ++ allSharesEntries ++ allLoansEntries ++ allOtherAssetsEntries ++
          allMoneyBorrowedEntries
    } yield Ok(view(viewModel(srn, allEntries), ""))).value.map(_.merge)
  }

  def viewModel(
    srn: Srn,
    entries: List[SummaryPageEntry]
  ): FormPageViewModel[List[SummaryPageEntry]] = FormPageViewModel[List[SummaryPageEntry]](
    Message("nonsipp.summary.title"),
    Message("nonsipp.summary.heading"),
    entries,
    routes.PsaDeclarationController.onPageLoad(srn)
  )
}
