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

package controllers.nonsipp.bondsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds.{CostOfBondsPage, NameOfBondsPage, WhyDoesSchemeHoldBondsPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import controllers.nonsipp.bondsdisposal.BondsDisposalCYAController._
import models.PointOfEntry.NoPointOfEntry
import models.HowDisposed._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal._
import viewmodels.Margin
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class BondsDisposalCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      saveService.save(
        request.userAnswers
          .set(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex), NoPointOfEntry)
          .getOrElse(request.userAnswers)
      )

      (
        for {
          bondsName <- request.userAnswers
            .get(NameOfBondsPage(srn, bondIndex))
            .getOrRecoverJourney
          acquisitionType <- request.userAnswers
            .get(WhyDoesSchemeHoldBondsPage(srn, bondIndex))
            .getOrRecoverJourney
          costOfBonds <- request.userAnswers
            .get(CostOfBondsPage(srn, bondIndex))
            .getOrRecoverJourney

          howBondsDisposed <- request.userAnswers
            .get(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex))
            .getOrRecoverJourney

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
            .getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName

        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
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
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers.set(BondsDisposalCompletedPage(srn, bondIndex, disposalIndex), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(BondsDisposalCompletedPage(srn, bondIndex, disposalIndex), mode, request.userAnswers)
          )
      )
    }
}

case class ViewModelParameters(
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

object BondsDisposalCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode.fold(
        normal = "bondsDisposalCYA.title",
        check = "bondsDisposalCYA.change.title"
      ),
      heading = parameters.mode.fold(
        normal = "bondsDisposalCYA.heading",
        check = Message("bondsDisposalCYA.change.heading")
      ),
      description = None,
      page = CheckYourAnswersViewModel
        .singleSection(
          rows(parameters)
        )
        .withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = parameters.mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue"
      ),
      onSubmit = routes.BondsDisposalCYAController
        .onSubmit(parameters.srn, parameters.bondIndex, parameters.disposalIndex, parameters.mode)
    )

  private def rows(parameters: ViewModelParameters): List[CheckYourAnswersRowViewModel] =
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
