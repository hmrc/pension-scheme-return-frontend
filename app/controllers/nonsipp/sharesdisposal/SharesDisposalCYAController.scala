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

package controllers.nonsipp.sharesdisposal

import cats.implicits.toShow
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.SharesDisposalCYAController._
import models.HowSharesDisposed._
import models.PointOfEntry.NoPointOfEntry
import models.SchemeHoldShare.Transfer
import models.{CheckMode, IdentityType, Mode, Money, SchemeHoldShare, TypeOfShares}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.shares.{
  CompanyNameRelatedSharesPage,
  TypeOfSharesHeldPage,
  WhenDidSchemeAcquireSharesPage,
  WhyDoesSchemeHoldSharesPage
}
import pages.nonsipp.sharesdisposal._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.Margin
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class SharesDisposalCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      // Clear any PointOfEntry
      saveService.save(
        request.userAnswers
          .set(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), NoPointOfEntry)
          .getOrElse(request.userAnswers)
      )

      (
        for {
          // Row 1 (Shares data)
          sharesType <- request.userAnswers
            .get(TypeOfSharesHeldPage(srn, shareIndex))
            .getOrRecoverJourney
          companyName <- request.userAnswers
            .get(CompanyNameRelatedSharesPage(srn, shareIndex))
            .getOrRecoverJourney
          acquisitionType <- request.userAnswers
            .get(WhyDoesSchemeHoldSharesPage(srn, shareIndex))
            .getOrRecoverJourney

          acquisitionDate = Option.when(acquisitionType != Transfer)(
            request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, shareIndex)).get
          )

          // Row 2 onwards (Shares Disposal data)
          howSharesDisposed <- request.userAnswers
            .get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney

          // <Rows conditional on Sold>
          dateSharesSold = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex)).get
          )
          numberSharesSold = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(HowManySharesSoldPage(srn, shareIndex, disposalIndex)).get
          )
          considerationSharesSold = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex)).get
          )
          buyerIdentity = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex)).get
          )
          buyerName = Option.when(howSharesDisposed == Sold)(
            List(
              request.userAnswers.get(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers.get(CompanyBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers.get(PartnershipBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers
                .get(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex))
                .map(_.name)
            ).flatten.head
          )
          buyerDetails = Option.when(howSharesDisposed == Sold)(
            List(
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex))
                .map(_.description)
            ).flatten.headOption
          )
          buyerReasonNoDetails = Option.when(howSharesDisposed == Sold)(
            List(
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, shareIndex, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value))
            ).flatten.headOption
          )
          isBuyerConnectedParty = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex)).get
          )
          isIndependentValuation = Option.when(howSharesDisposed == Sold)(
            request.userAnswers.get(IndependentValuationPage(srn, shareIndex, disposalIndex)).get
          )

          // <Rows conditional on Redeemed>
          dateSharesRedeemed = Option.when(howSharesDisposed == Redeemed)(
            request.userAnswers.get(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex)).get
          )
          numberSharesRedeemed = Option.when(howSharesDisposed == Redeemed)(
            request.userAnswers.get(HowManySharesRedeemedPage(srn, shareIndex, disposalIndex)).get
          )
          considerationSharesRedeemed = Option.when(howSharesDisposed == Redeemed)(
            request.userAnswers.get(TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex)).get
          )

          // Last row
          sharesStillHeld <- request.userAnswers
            .get(HowManySharesPage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName

        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                sharesType,
                companyName,
                acquisitionType,
                acquisitionDate,
                howSharesDisposed,
                dateSharesSold,
                numberSharesSold,
                considerationSharesSold,
                buyerIdentity,
                buyerName,
                buyerDetails.flatten,
                buyerReasonNoDetails.flatten,
                isBuyerConnectedParty,
                isIndependentValuation,
                dateSharesRedeemed,
                numberSharesRedeemed,
                considerationSharesRedeemed,
                sharesStillHeld,
                schemeName,
                mode
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers.set(SharesDisposalCompletedPage(srn, shareIndex, disposalIndex), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(SharesDisposalCompletedPage(srn, shareIndex, disposalIndex), mode, request.userAnswers)
          )
      )
    }
}

case class ViewModelParameters(
  srn: Srn,
  shareIndex: Max5000,
  disposalIndex: Max50,
  sharesType: TypeOfShares,
  companyName: String,
  acquisitionType: SchemeHoldShare,
  acquisitionDate: Option[LocalDate],
  howSharesDisposed: HowSharesDisposed,
  dateSharesSold: Option[LocalDate],
  numberSharesSold: Option[Int],
  considerationSharesSold: Option[Money],
  buyerIdentity: Option[IdentityType],
  buyerName: Option[String],
  buyerDetails: Option[String],
  buyerReasonNoDetails: Option[String],
  isBuyerConnectedParty: Option[Boolean],
  isIndependentValuation: Option[Boolean],
  dateSharesRedeemed: Option[LocalDate],
  numberSharesRedeemed: Option[Int],
  considerationSharesRedeemed: Option[Money],
  sharesStillHeld: Int,
  schemeName: String,
  mode: Mode
)

object SharesDisposalCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode.fold(
        normal = "sharesDisposal.cya.title",
        check = "sharesDisposal.change.title"
      ),
      heading = parameters.mode.fold(
        normal = "sharesDisposal.cya.heading",
        check = Message("sharesDisposal.change.heading", parameters.companyName)
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
      onSubmit = routes.SharesDisposalCYAController
        .onSubmit(parameters.srn, parameters.shareIndex, parameters.disposalIndex, parameters.mode)
    )

  private def rows(parameters: ViewModelParameters): List[CheckYourAnswersRowViewModel] =
    firstRow(
      parameters.sharesType,
      parameters.companyName,
      parameters.acquisitionType,
      parameters.acquisitionDate
    ) ++
      secondRow(
        parameters.srn,
        parameters.shareIndex,
        parameters.disposalIndex,
        parameters.howSharesDisposed
      ) ++
      (parameters.howSharesDisposed match {
        case Sold =>
          conditionalSoldRows(
            parameters.srn,
            parameters.shareIndex,
            parameters.disposalIndex,
            parameters.companyName,
            parameters.dateSharesSold.get,
            parameters.numberSharesSold.get,
            parameters.considerationSharesSold.get,
            parameters.buyerIdentity.get,
            parameters.buyerName.get,
            parameters.buyerDetails,
            parameters.buyerReasonNoDetails,
            parameters.isBuyerConnectedParty.get,
            parameters.isIndependentValuation.get
          )
        case Redeemed =>
          conditionalRedeemedRows(
            parameters.srn,
            parameters.shareIndex,
            parameters.disposalIndex,
            parameters.companyName,
            parameters.dateSharesRedeemed.get,
            parameters.numberSharesRedeemed.get,
            parameters.considerationSharesRedeemed.get
          )
        case Transferred =>
          List.empty
        case Other(otherDetails) =>
          conditionalOtherRow(parameters.srn, parameters.shareIndex, parameters.disposalIndex, otherDetails)
      }) ++
      lastRow(
        parameters.srn,
        parameters.shareIndex,
        parameters.disposalIndex,
        parameters.companyName,
        parameters.schemeName,
        parameters.sharesStillHeld
      )

  private def firstRow(
    sharesType: TypeOfShares,
    companyName: String,
    acquisitionType: SchemeHoldShare,
    acquisitionDate: Option[LocalDate]
  ): List[CheckYourAnswersRowViewModel] = {

    val sharesTypeString = sharesType match {
      case TypeOfShares.SponsoringEmployer => "sharesDisposal.cya.typeOfShares.sponsoringEmployer"
      case TypeOfShares.Unquoted => "sharesDisposal.cya.typeOfShares.unquoted"
      case TypeOfShares.ConnectedParty => "sharesDisposal.cya.typeOfShares.connectedParty"
    }

    val acquisitionTypeString = acquisitionType match {
      case SchemeHoldShare.Acquisition => "sharesDisposal.cya.methodOfAcquisition.acquired"
      case SchemeHoldShare.Contribution => "sharesDisposal.cya.methodOfAcquisition.contributed"
      case SchemeHoldShare.Transfer => "sharesDisposal.cya.methodOfAcquisition.transferred"
    }

    val row1ValueMessage: Message = acquisitionDate match {
      case Some(dateOfAcquisition) =>
        Message(
          "sharesDisposal.cya.baseRow1.value.withDate",
          sharesTypeString,
          companyName,
          acquisitionTypeString,
          dateOfAcquisition.show
        )
      case None =>
        Message(
          "sharesDisposal.cya.baseRow1.value",
          sharesTypeString,
          companyName,
          acquisitionTypeString
        )
    }

    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.baseRow1.key"),
        row1ValueMessage
      )
    )
  }

  private def secondRow(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    howSharesDisposed: HowSharesDisposed
  ): List[CheckYourAnswersRowViewModel] = {
    val howSharesDisposedName = howSharesDisposed match {
      case Sold => Sold.name
      case Redeemed => Redeemed.name
      case Transferred => Transferred.name
      case Other(_) => Other.name
    }
    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.baseRow2.key"),
        howSharesDisposedName
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.baseRow2.hidden")
      )
    )
  }

  private def conditionalSoldRows(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    dateSharesSold: LocalDate,
    numberSharesSold: Int,
    considerationSharesSold: Money,
    buyerIdentity: IdentityType,
    buyerName: String,
    buyerDetails: Option[String],
    buyerReasonNoDetails: Option[String],
    isBuyerConnectedParty: Boolean,
    isIndependentValuation: Boolean
  ): List[CheckYourAnswersRowViewModel] = {

    val buyerIdentityMessage = buyerIdentity match {
      case IdentityType.Individual => "sharesDisposal.whoWereTheSharesSoldTo.radioList1"
      case IdentityType.UKCompany => "sharesDisposal.whoWereTheSharesSoldTo.radioList2"
      case IdentityType.UKPartnership => "sharesDisposal.whoWereTheSharesSoldTo.radioList3"
      case IdentityType.Other => "sharesDisposal.whoWereTheSharesSoldTo.radioList4"
    }

    val buyerNameUrl = buyerIdentity match {
      case IdentityType.Individual =>
        routes.SharesIndividualBuyerNameController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
      case IdentityType.UKCompany =>
        routes.CompanyNameOfSharesBuyerController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
      case IdentityType.UKPartnership =>
        routes.PartnershipBuyerNameController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
      case IdentityType.Other =>
        routes.OtherBuyerDetailsController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
    }

    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow1.key"),
        dateSharesSold.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalSoldRow1.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow2.key", companyName),
        numberSharesSold
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.conditionalSoldRow2.hidden", companyName))
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow3.key"),
        s"£${considerationSharesSold.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesSoldController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalSoldRow3.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow4.key", companyName),
        buyerIdentityMessage
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.WhoWereTheSharesSoldToController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.conditionalSoldRow4.hidden", companyName))
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow5.key"),
        buyerName
      ).withAction(
        SummaryAction(
          "site.change",
          buyerNameUrl
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalSoldRow5.hidden")
      ),
      conditionalSoldRow6(
        srn,
        shareIndex,
        disposalIndex,
        buyerIdentity,
        buyerName,
        buyerDetails,
        buyerReasonNoDetails
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow7.key", buyerName),
        if (isBuyerConnectedParty) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.conditionalSoldRow7.hidden", buyerName))
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalSoldRow8.key", companyName),
        if (isIndependentValuation) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.IndependentValuationController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.conditionalSoldRow8.hidden", companyName))
      )
    )
  }

  private def conditionalSoldRow6(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    buyerIdentity: IdentityType,
    buyerName: String,
    buyerDetails: Option[String],
    buyerReasonNoDetails: Option[String]
  ): CheckYourAnswersRowViewModel = buyerIdentity match {
    // Individual
    case IdentityType.Individual =>
      (buyerDetails, buyerReasonNoDetails) match {
        // NINO provided
        case (Some(details), None) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.details.key",
              buyerName,
              Message("sharesDisposal.cya.nino")
            ),
            details
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IndividualBuyerNinoNumberController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.details.hidden", "sharesDisposal.cya.nino")
            )
          )
        // NINO not provided
        case (None, Some(reason)) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.noDetails.key",
              buyerName,
              Message("sharesDisposal.cya.nino")
            ),
            reason
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IndividualBuyerNinoNumberController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.noDetails.hidden", "sharesDisposal.cya.nino")
            )
          )
      }
    // Company
    case IdentityType.UKCompany =>
      (buyerDetails, buyerReasonNoDetails) match {
        // CRN provided
        case (Some(details), None) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.details.key",
              buyerName,
              Message("sharesDisposal.cya.crn")
            ),
            details
          ).withAction(
            SummaryAction(
              "site.change",
              routes.CompanyBuyerCrnController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.details.hidden", "sharesDisposal.cya.crn")
            )
          )
        // CRN not provided
        case (None, Some(reason)) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.noDetails.key",
              buyerName,
              Message("sharesDisposal.cya.crn")
            ),
            reason
          ).withAction(
            SummaryAction(
              "site.change",
              routes.CompanyBuyerCrnController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.noDetails.hidden", "sharesDisposal.cya.crn")
            )
          )
      }
    // Partnership
    case IdentityType.UKPartnership =>
      (buyerDetails, buyerReasonNoDetails) match {
        // UTR provided
        case (Some(details), None) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.details.key",
              buyerName,
              Message("sharesDisposal.cya.utr")
            ),
            details
          ).withAction(
            SummaryAction(
              "site.change",
              routes.PartnershipBuyerUtrController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.details.hidden", "sharesDisposal.cya.utr")
            )
          )
        // UTR not provided
        case (None, Some(reason)) =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesDisposal.cya.conditionalSoldRow6.noDetails.key",
              buyerName,
              Message("sharesDisposal.cya.utr")
            ),
            reason
          ).withAction(
            SummaryAction(
              "site.change",
              routes.PartnershipBuyerUtrController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
            ).withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.noDetails.hidden", "sharesDisposal.cya.utr")
            )
          )
      }
    // Other
    case IdentityType.Other =>
      CheckYourAnswersRowViewModel(
        Message(
          "sharesDisposal.cya.conditionalSoldRow6.details.key",
          buyerName,
          Message("sharesDisposal.cya.details")
        ),
        buyerDetails.get
      ).withAction(
        SummaryAction(
          "site.change",
          routes.OtherBuyerDetailsController.onSubmit(srn, shareIndex, disposalIndex, CheckMode).url
        ).withVisuallyHiddenContent(
          Message("sharesDisposal.cya.conditionalSoldRow6.details.hidden", "sharesDisposal.cya.details")
        )
      )
  }

  private def conditionalRedeemedRows(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    dateSharesRedeemed: LocalDate,
    numberSharesRedeemed: Int,
    considerationSharesRedeemed: Money
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalRedeemedRow1.key"),
        dateSharesRedeemed.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalRedeemedRow1.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalRedeemedRow2.key", companyName),
        numberSharesRedeemed
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.HowManySharesRedeemedController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.conditionalRedeemedRow2.hidden", companyName))
      ),
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalRedeemedRow3.key"),
        s"£${considerationSharesRedeemed.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesRedeemedController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalRedeemedRow3.hidden")
      )
    )

  private def conditionalOtherRow(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    otherDetails: String
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.conditionalOtherRow1.key"),
        otherDetails
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("sharesDisposal.cya.conditionalOtherRow1.hidden")
      )
    )

  private def lastRow(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    schemeName: String,
    sharesStillHeld: Int
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("sharesDisposal.cya.baseRow3.key", companyName, schemeName),
        sharesStillHeld
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.sharesdisposal.routes.HowManySharesController
            .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(Message("sharesDisposal.cya.baseRow3.hidden", companyName, schemeName))
      )
    )
}
