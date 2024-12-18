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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import models.PointOfEntry.NoPointOfEntry
import utils.ListUtils.ListOps
import controllers.nonsipp.sharesdisposal.SharesDisposalCYAController._
import models.SchemeHoldShare.Transfer
import cats.implicits.toShow
import config.Constants.maxDisposalsPerShare
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal._
import models.requests.DataRequest
import pages.nonsipp.shares._
import play.api.mvc._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import models.HowSharesDisposed._
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.MessagesApi
import viewmodels.Margin
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

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
    identifyAndRequireData(srn).async { implicit request =>
      // Clear any PointOfEntry
      saveService
        .save(
          request.userAnswers
            .set(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), NoPointOfEntry)
            .set(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)
            .getOrElse(request.userAnswers)
        )
      onPageLoadCommon(srn, shareIndex, disposalIndex, mode)

    }

  def onPageLoadViewOnly(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, shareIndex, disposalIndex, mode)
    }

  def onPageLoadCommon(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode)(
    implicit request: DataRequest[AnyContent]
  ): Future[Result] =
    (
      for {
        // Row 1 (Shares data)
        sharesType <- request.userAnswers
          .get(TypeOfSharesHeldPage(srn, shareIndex))
          .getOrRecoverJourneyT
        companyName <- request.userAnswers
          .get(CompanyNameRelatedSharesPage(srn, shareIndex))
          .getOrRecoverJourneyT
        acquisitionType <- request.userAnswers
          .get(WhyDoesSchemeHoldSharesPage(srn, shareIndex))
          .getOrRecoverJourneyT

        acquisitionDate = Option.when(acquisitionType != Transfer)(
          request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, shareIndex)).get
        )
        // Row 2 onwards (Shares Disposal data)
        howSharesDisposed <- request.userAnswers
          .get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))
          .getOrRecoverJourneyT
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

        sharesStillHeld <- request.userAnswers
          .get(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex))
          .getOrRecoverJourneyT

        schemeName = request.schemeDetails.schemeName

        disposalAmount = request.userAnswers
          .map(SharesDisposalProgress.all(srn, shareIndex))
          .count { case (_, progress) => progress.completed }

      } yield {
        val isMaximumReached = disposalAmount >= maxDisposalsPerShare

        Ok(
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
              ),
              viewOnlyUpdated = false,
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
              isMaximumReached = isMaximumReached
            )
          )
        )
      }
    ).merge

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- request.userAnswers
          .set(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
            .onPageLoad(srn, shareIndex, disposalIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(SharesDisposalCYAPage(srn), mode, request.userAnswers)
          )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
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
  def viewModel(
    parameters: ViewModelParameters,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    isMaximumReached: Boolean
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = parameters.mode,
      title = parameters.mode.fold(
        normal = "sharesDisposal.cya.title",
        check = "sharesDisposal.change.title",
        viewOnly = "sharesDisposal.viewOnly.title"
      ),
      heading = parameters.mode.fold(
        normal = "sharesDisposal.cya.heading",
        check = Message("sharesDisposal.change.heading", parameters.companyName),
        viewOnly = "sharesDisposal.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel
        .singleSection(
          rows(parameters)
        )
        .withMarginBottom(Margin.Fixed60Bottom)
        .withInset(Option.when(isMaximumReached)(Message("sharesDisposal.cya.inset.maximumReached"))),
      refresh = None,
      buttonText =
        parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
        .onSubmit(parameters.srn, parameters.shareIndex, parameters.disposalIndex, parameters.mode),
      optViewOnlyDetails = if (parameters.mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "sharesDisposal.viewOnly.title",
            heading = Message("sharesDisposal.viewOnly.heading", parameters.companyName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onSubmitViewOnly(parameters.srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onSubmit(parameters.srn, parameters.shareIndex, parameters.disposalIndex, parameters.mode)
            }
          )
        )
      } else {
        None
      }
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
            .url + "#howWereSharesDisposed"
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
        controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.Other =>
        controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url + "#buyerName"
    }

    val buyerDetailsUrl = buyerIdentity match {
      case IdentityType.Individual =>
        controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url
      case IdentityType.Other =>
        controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
          .onSubmit(srn, shareIndex, disposalIndex, CheckMode)
          .url + "#buyerDetails"
    }

    val buyerDetailsType = buyerIdentity match {
      case IdentityType.Individual => Message("sharesDisposal.cya.nino")
      case IdentityType.UKCompany => Message("sharesDisposal.cya.crn")
      case IdentityType.UKPartnership => Message("sharesDisposal.cya.utr")
      case IdentityType.Other => Message("sharesDisposal.cya.details")
    }

    val buyerNoDetailsHidden = buyerIdentity match {
      case IdentityType.Other => Message("")
      case _ => Message("sharesDisposal.cya.conditionalSoldRow6.noDetails.hidden", buyerDetailsType)
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
      )
    ) :?+ buyerDetails.map(
      details =>
        CheckYourAnswersRowViewModel(
          Message("sharesDisposal.cya.conditionalSoldRow6.details.key", buyerName, buyerDetailsType),
          details
        ).withAction(
          SummaryAction("site.change", buyerDetailsUrl)
            .withVisuallyHiddenContent(
              Message("sharesDisposal.cya.conditionalSoldRow6.details.hidden", buyerDetailsType)
            )
        )
    ) :?+ buyerReasonNoDetails.map(
      reasonNoDetails =>
        CheckYourAnswersRowViewModel(
          Message("sharesDisposal.cya.conditionalSoldRow6.noDetails.key", buyerName, buyerDetailsType),
          reasonNoDetails
        ).withAction(
          SummaryAction("site.change", buyerDetailsUrl)
            .withVisuallyHiddenContent(buyerNoDetailsHidden)
        )
    ) :+
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
      ) :+
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
            .url + "#otherDetails"
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
