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

import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import utils.nonsipp.summary._
import controllers.PSRController
import cats.implicits.{toShow, _}
import controllers.actions.IdentifyAndRequireData
import play.api.i18n._
import views.html.SummaryView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, NormalMode}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SummaryPageEntry, _}

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class PreSubmissionSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val schemeDateService: SchemeDateService,
  val saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val landOrPropertyDisposalCheckAnswersUtils = LandOrPropertyDisposalCheckAnswersUtils(saveService)
    val loansCheckAnswersUtils = LoansCheckAnswersUtils(schemeDateService)

    val schemeDate = schemeDateService
      .taxYearOrAccountingPeriods(srn)
      .map(_.map(x => DateRange(x.head._1.from, x.reverse.head._1.to)).merge)
      .map(x => Message("nonsipp.summary.caption", x.from.show, x.to.show))

    List(
      EmployerContributionsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      TransfersInCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      TransfersOutCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      LandOrPropertyCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      landOrPropertyDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      BondsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      BondsDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      SharesCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      SharesDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      loansCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      OtherAssetsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      OtherAssetsDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      MoneyBorrowedCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
    ).sequence
      .map(_.flatten)
      .map(entries => Ok(view(viewModel(srn, entries), schemeDate)))
      .value
      .map(_.merge)
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
