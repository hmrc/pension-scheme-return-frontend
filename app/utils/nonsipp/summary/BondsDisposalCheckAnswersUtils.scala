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
import models.HowDisposed._
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.toShow
import config.Constants.maxDisposalPerBond
import uk.gov.hmrc.http.HeaderCarrier
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models.{HowDisposed, _}
import pages.nonsipp.bondsdisposal._
import viewmodels.{DisplayMessage, Margin}
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

case class BondsDisposalViewModelParameters(
  srn: Srn,
  bondIndex: Max5000,
  disposalIndex: Max50,
  bondsName: String,
  acquisitionType: SchemeHoldBond,
  costOfBonds: Money,
  howBondsDisposed: HowDisposed,
  dateBondsSold: Option[LocalDate],
  considerationBondsSold: Option[Money],
  buyerName: Option[String],
  isBuyerConnectedParty: Option[Boolean],
  bondsStillHeld: Int,
  schemeName: String,
  mode: Mode
)

type BondsDisposalData = (
  parameters: BondsDisposalViewModelParameters,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int],
  isMaximumReached: Boolean
)

object BondsDisposalCheckAnswersUtils
    extends CheckAnswersUtils[(Max5000, Max50), BondsDisposalData]
    with PsrControllerHelpers {
  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(BondsDisposalPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.bonds.disposals.heading"))

  override def subheading(data: BondsDisposalData): Option[DisplayMessage] =
    Some(Message("nonsipp.summary.bonds.disposals.subheading", data.parameters.bondsName))

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[(Max5000, Max50)] =
    request.userAnswers
      .map(BondsDisposalProgress.all())
      .flatMap { case (bIndex, disposals) =>
        disposals.map { case (dIndex, progress) =>
          ((bIndex, dIndex), progress)
        }
      }
      .keys
      .toList
      .flatMap((bi, di) =>
        for {
          refinedBi <- refineStringIndex[Max5000.Refined](bi)
          refinedDi <- refineStringIndex[Max50.Refined](di)
        } yield (refinedBi, refinedDi)
      )

  override def summaryDataAsync(srn: Srn, index: (Max5000, Max50), mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, BondsDisposalData]] = {
    val (bondIndex, disposalIndex) = index
    (for {
      bondsName <- request.userAnswers
        .get(NameOfBondsPage(srn, bondIndex))
        .getOrRecoverJourneyT
      acquisitionType <- request.userAnswers
        .get(WhyDoesSchemeHoldBondsPage(srn, bondIndex))
        .getOrRecoverJourneyT
      costOfBonds <- request.userAnswers
        .get(CostOfBondsPage(srn, bondIndex))
        .getOrRecoverJourneyT

      howBondsDisposed <- request.userAnswers
        .get(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex))
        .getOrRecoverJourneyT

      dateBondsSold = Option.when(howBondsDisposed == Sold)(
        request.userAnswers.get(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex)).get
      )

      considerationBondsSold = Option.when(howBondsDisposed == Sold)(
        request.userAnswers.get(TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex)).get
      )
      buyerName = Option.when(howBondsDisposed == Sold)(
        request.userAnswers.get(BuyerNamePage(srn, bondIndex, disposalIndex)).get
      )
      isBuyerConnectedParty = Option.when(howBondsDisposed == Sold)(
        request.userAnswers.get(IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex)).get
      )

      bondsStillHeld <- request.userAnswers
        .get(BondsStillHeldPage(srn, bondIndex, disposalIndex))
        .getOrRecoverJourneyT

      disposalAmount = request.userAnswers
        .map(BondsDisposalProgress.all(bondIndex))
        .count { case (_, progress) => progress.completed }

      schemeName = request.schemeDetails.schemeName
      isMaximumReached = disposalAmount >= maxDisposalPerBond
    } yield (
      BondsDisposalViewModelParameters(
        srn,
        bondIndex,
        disposalIndex,
        bondsName,
        acquisitionType,
        costOfBonds,
        howBondsDisposed,
        dateBondsSold,
        considerationBondsSold,
        buyerName,
        isBuyerConnectedParty,
        bondsStillHeld,
        schemeName,
        mode
      ),
      false, // flag is not displayed on this tier
      request.year,
      request.currentVersion,
      request.previousVersion,
      isMaximumReached
    )).value
  }

  def viewModel(data: BondsDisposalData): FormPageViewModel[CheckYourAnswersViewModel] =
    BondsDisposalCheckAnswersUtils.viewModel(
      data.parameters,
      data.viewOnlyUpdated,
      data.optYear,
      data.optCurrentVersion,
      data.optPreviousVersion,
      data.isMaximumReached
    )

  def viewModel(
    parameters: BondsDisposalViewModelParameters,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    isMaximumReached: Boolean
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = parameters.mode,
      title = parameters.mode.fold(
        normal = "bondsDisposalCYA.title",
        check = "bondsDisposalCYA.change.title",
        viewOnly = "bondsDisposalCYA.viewOnly.title"
      ),
      heading = parameters.mode.fold(
        normal = "bondsDisposalCYA.heading",
        check = Message("bondsDisposalCYA.change.heading"),
        viewOnly = Message("bondsDisposalCYA.viewOnly.heading", parameters.bondsName)
      ),
      description = None,
      page = CheckYourAnswersViewModel
        .singleSection(
          rows(parameters)
        )
        .withMarginBottom(Margin.Fixed60Bottom)
        .withInset(Option.when(isMaximumReached)(Message("bondsDisposalCYA.inset.maximumReached"))),
      refresh = None,
      buttonText =
        parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
        .onSubmit(parameters.srn, parameters.bondIndex, parameters.disposalIndex, parameters.mode),
      optViewOnlyDetails = if (parameters.mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "bondsDisposalCYA.viewOnly.title",
            heading = Message("bondsDisposalCYA.viewOnly.heading", parameters.bondsName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                  .onSubmitViewOnly(parameters.srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                  .onSubmit(parameters.srn, parameters.bondIndex, parameters.disposalIndex, parameters.mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def rows(parameters: BondsDisposalViewModelParameters): List[CheckYourAnswersRowViewModel] =
    firstRow(
      parameters.bondsName,
      parameters.acquisitionType,
      parameters.costOfBonds
    ) ++
      secondRow(
        parameters.srn,
        parameters.bondIndex,
        parameters.disposalIndex,
        parameters.howBondsDisposed
      ) ++
      (parameters.howBondsDisposed match {
        case Sold =>
          conditionalSoldRows(
            parameters.srn,
            parameters.bondIndex,
            parameters.disposalIndex,
            parameters.dateBondsSold.get,
            parameters.considerationBondsSold.get,
            parameters.buyerName.get,
            parameters.isBuyerConnectedParty.get
          )
        case Transferred =>
          List.empty
        case Other(otherDetails) =>
          conditionalOtherRow(parameters.srn, parameters.bondIndex, parameters.disposalIndex, otherDetails)
      }) ++
      lastRow(
        parameters.srn,
        parameters.bondIndex,
        parameters.disposalIndex,
        parameters.bondsName,
        parameters.schemeName,
        parameters.bondsStillHeld
      )

  private def firstRow(
    bondsName: String,
    acquisitionType: SchemeHoldBond,
    costOfBonds: Money
  ): List[CheckYourAnswersRowViewModel] = {
    val acquisitionTypeString = acquisitionType match {
      case SchemeHoldBond.Acquisition => "bondsDisposal.BondsDisposalCYA.acquired"
      case SchemeHoldBond.Contribution => "bondsDisposal.BondsDisposalCYA.contributed"
      case SchemeHoldBond.Transfer => "bondsDisposal.BondsDisposalCYA.transferred"
    }

    val row1ValueMessage = Message(
      "bondsDisposal.cya.baseRow1.value",
      bondsName,
      acquisitionTypeString,
      costOfBonds.displayAs
    )

    List(
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.baseRow1.key"),
        row1ValueMessage
      )
    )
  }

  private def secondRow(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    howBondsDisposed: HowDisposed
  ): List[CheckYourAnswersRowViewModel] = {
    val howBondsDisposedName = howBondsDisposed match {
      case HowDisposed.Sold => Sold.name
      case HowDisposed.Transferred => Transferred.name
      case HowDisposed.Other(_) => Other.name
    }
    List(
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.baseRow2.key"),
        howBondsDisposedName
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url + "#howWereBondsDisposedOf"
        ).withVisuallyHiddenContent("bondsDisposal.cya.baseRow2.hidden")
      )
    )
  }

  private def conditionalSoldRows(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    dateBondsSold: LocalDate,
    considerationBondsSold: Money,
    buyerName: String,
    isBuyerConnectedParty: Boolean
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.conditionalSoldRow1.key"),
        dateBondsSold.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.WhenWereBondsSoldController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("bondsDisposal.cya.conditionalSoldRow1.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.conditionalSoldRow2.key"),
        s"£${considerationBondsSold.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.TotalConsiderationSaleBondsController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("bondsDisposal.cya.conditionalSoldRow2.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.conditionalSoldRow3.key"),
        buyerName.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.BuyerNameController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("bondsDisposal.cya.conditionalSoldRow3.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.conditionalSoldRow4.key", buyerName.show),
        if (isBuyerConnectedParty) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("bondsDisposal.cya.conditionalSoldRow4.hidden", buyerName.show))
      )
    )

  private def conditionalOtherRow(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    otherDetails: String
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.conditionalOtherRow1.key"),
        otherDetails
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url + "#otherDetails"
        ).withVisuallyHiddenContent("bondsDisposal.cya.conditionalOtherRow1.hidden")
      )
    )

  private def lastRow(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    bondsName: String,
    schemeName: String,
    bondsStillHeld: Int
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("bondsDisposal.cya.baseRow3.key", bondsName, schemeName),
        bondsStillHeld
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
            .onSubmit(srn, bondIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("bondsDisposal.cya.baseRow3.hidden", bondsName, schemeName))
      )
    )
}
