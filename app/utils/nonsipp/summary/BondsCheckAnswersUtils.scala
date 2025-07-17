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

package utils.nonsipp.summary

import pages.nonsipp.bonds._
import viewmodels.implicits._
import play.api.mvc._
import models.SchemeId.Srn
import utils.IntUtils.toRefined5000
import cats.implicits.toShow
import models.requests.DataRequest
import controllers.PsrControllerHelpers
import models.SchemeHoldBond.{Acquisition, Contribution, Transfer}
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import java.time.LocalDate

object BondsCheckAnswersUtils extends PsrControllerHelpers {

  def summaryData(srn: Srn, index: Int, mode: Mode)(using request: DataRequest[AnyContent]): Either[
    Result,
    (
      srn: Srn,
      index: Int,
      schemeName: String,
      nameOfBonds: String,
      whyDoesSchemeHoldBonds: SchemeHoldBond,
      whenDidSchemeAcquireBonds: Option[LocalDate],
      costOfBonds: Money,
      bondsFromConnectedParty: Option[Boolean],
      areBondsUnregulated: Boolean,
      incomeFromBonds: Money,
      mode: Mode,
      viewOnlyUpdated: Boolean,
      optYear: Option[String],
      optCurrentVersion: Option[Int],
      optPreviousVersion: Option[Int]
    )
  ] =
    for {
      nameOfBonds <- request.userAnswers.get(NameOfBondsPage(srn, index)).getOrRecoverJourney
      whyDoesSchemeHoldBonds <- request.userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)).getOrRecoverJourney

      whenDidSchemeAcquireBonds = Option.when(whyDoesSchemeHoldBonds != Transfer)(
        request.userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, index)).get
      )

      costOfBonds <- request.userAnswers.get(CostOfBondsPage(srn, index)).getOrRecoverJourney

      bondsFromConnectedParty = Option.when(whyDoesSchemeHoldBonds == Acquisition)(
        request.userAnswers.get(BondsFromConnectedPartyPage(srn, index)).get
      )

      areBondsUnregulated <- request.userAnswers.get(AreBondsUnregulatedPage(srn, index)).getOrRecoverJourney

      incomeFromBonds <- request.userAnswers.get(IncomeFromBondsPage(srn, index)).getOrRecoverJourney

      schemeName = request.schemeDetails.schemeName
    } yield (
      srn,
      index,
      schemeName,
      nameOfBonds,
      whyDoesSchemeHoldBonds,
      whenDidSchemeAcquireBonds,
      costOfBonds,
      bondsFromConnectedParty,
      areBondsUnregulated,
      incomeFromBonds,
      mode,
      false,
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  def viewModel(
    srn: Srn,
    index: Int,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    costOfBonds: Money,
    bondsFromConnectedParty: Option[Boolean],
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "bonds.checkYourAnswers.title",
        check = "bonds.checkYourAnswers.change.title",
        viewOnly = "bonds.checkYourAnswers.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "bonds.checkYourAnswers.heading",
        check = "bonds.checkYourAnswers.change.heading",
        viewOnly = Message("bonds.checkYourAnswers.viewOnly.heading", nameOfBonds)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          schemeName,
          nameOfBonds,
          whyDoesSchemeHoldBonds,
          whenDidSchemeAcquireBonds,
          costOfBonds,
          bondsFromConnectedParty,
          areBondsUnregulated,
          incomeFromBonds,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue",
        viewOnly = "site.continue"
      ),
      onSubmit =
        controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "bonds.checkYourAnswers.viewOnly.title",
            heading = Message("bonds.checkYourAnswers.viewOnly.heading", nameOfBonds),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    index: Int,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    costOfBonds: Money,
    bondsFromConnectedParty: Option[Boolean],
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      whyDoesSchemeHoldBonds match {
        case Acquisition =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              List(whenDidSchemeAcquireBondsRow(srn, index, schemeName, whenDidSchemeAcquireBonds, mode)) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              List(bondsFromConnectedPartyRow(srn, index, bondsFromConnectedParty, mode)) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
        case Contribution =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              List(whenDidSchemeAcquireBondsRow(srn, index, schemeName, whenDidSchemeAcquireBonds, mode)) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
        case Transfer =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
      }
    )

  private def whenDidSchemeAcquireBondsRow(
    srn: Srn,
    index: Int,
    schemeName: String,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds", schemeName),
      whenDidSchemeAcquireBonds.get.show
    ).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.WhenDidSchemeAcquireBondsController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent(("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds.hidden", schemeName))
    )

  private def bondsFromConnectedPartyRow(
    srn: Srn,
    index: Int,
    bondsFromConnectedParty: Option[Boolean],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.bondsFromConnectedParty", bondsFromConnectedParty.show),
      if (bondsFromConnectedParty.get) "site.yes" else "site.no"
    ).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.BondsFromConnectedPartyController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.bondsFromConnectedParty.hidden")
    )

  private def unconditionalRowsPart1(
    srn: Srn,
    index: Int,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("bonds.checkYourAnswers.section.nameOfBonds", nameOfBonds.show).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.NameOfBondsController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.nameOfBonds.hidden")
    ),
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.whyDoesSchemeHoldBonds", schemeName),
      whyDoesSchemeHoldBonds match {
        case Acquisition => "bonds.checkYourAnswers.acquisition"
        case Contribution => "bonds.checkYourAnswers.contribution"
        case Transfer => "bonds.checkYourAnswers.transfer"
      }
    ).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.WhyDoesSchemeHoldBondsController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent(("bonds.checkYourAnswers.section.whyDoesSchemeHoldBonds.hidden", schemeName))
    )
  )

  private def unconditionalRowsPart2(
    srn: Srn,
    index: Int,
    costOfBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("bonds.checkYourAnswers.section.costOfBonds", s"£${costOfBonds.displayAs}")
      .withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bonds.routes.CostOfBondsController.onSubmit(srn, index, mode).url
        )
          .withVisuallyHiddenContent("bonds.checkYourAnswers.section.costOfBonds.hidden")
      )
  )

  private def unconditionalRowsPart3(
    srn: Srn,
    index: Int,
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.areBondsUnregulated", areBondsUnregulated.show),
      if (areBondsUnregulated) "site.yes" else "site.no"
    ).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.AreBondsUnregulatedController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.areBondsUnregulated.hidden")
    ),
    CheckYourAnswersRowViewModel(
      "bonds.checkYourAnswers.section.incomeFromBonds",
      s"£${incomeFromBonds.displayAs}"
    ).withAction(
      SummaryAction(
        "site.change",
        controllers.nonsipp.bonds.routes.IncomeFromBondsController.onSubmit(srn, index, mode).url
      )
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.incomeFromBonds.hidden")
    )
  )
}
