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

package controllers.nonsipp.otherassetsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import utils.IntUtils.{toInt, IntOpts}
import cats.implicits.toShow
import config.Constants.maxDisposalPerOtherAsset
import controllers.actions._
import models.requests.DataRequest
import pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage
import models.HowDisposed._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import controllers.nonsipp.otherassetsdisposal.AssetDisposalCYAController._
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n._
import viewmodels.Margin
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

class AssetDisposalCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    disposalIndex: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      onPageLoadCommon(srn, index.refined, disposalIndex.refined, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    disposalIndex: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, index.refined, disposalIndex.refined, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode)(
    implicit request: DataRequest[AnyContent]
  ): Future[Result] =
    (
      for {
        updatedUserAnswers <- request.userAnswers
          .set(OtherAssetsDisposalProgress(srn, index, disposalIndex), SectionJourneyStatus.Completed)
          .toOption
          .getOrRecoverJourneyT

        howWasAssetDisposed <- updatedUserAnswers
          .get(HowWasAssetDisposedOfPage(srn, index, disposalIndex))
          .getOrRecoverJourneyT
        whatIsOtherAsset <- updatedUserAnswers
          .get(WhatIsOtherAssetPage(srn, index))
          .getOrRecoverJourneyT
        anyPartAssetStillHeld <- updatedUserAnswers
          .get(AnyPartAssetStillHeldPage(srn, index, disposalIndex))
          .getOrRecoverJourneyT

        totalConsiderationSale = Option.when(howWasAssetDisposed == Sold)(
          updatedUserAnswers.get(TotalConsiderationSaleAssetPage(srn, index, disposalIndex)).get
        )
        independentValuation = Option.when(howWasAssetDisposed == Sold)(
          updatedUserAnswers.get(AssetSaleIndependentValuationPage(srn, index, disposalIndex)).get
        )
        assetDisposedType = Option.when(howWasAssetDisposed == Sold)(
          updatedUserAnswers
            .get(TypeOfAssetBuyerPage(srn: Srn, index, disposalIndex))
            .get
        )

        whenWasAssetSold = Option.when(howWasAssetDisposed == Sold)(
          updatedUserAnswers.get(WhenWasAssetSoldPage(srn, index, disposalIndex)).get
        )

        assetDisposalBuyerConnectedParty = Option.when(howWasAssetDisposed == Sold)(
          updatedUserAnswers.get(IsBuyerConnectedPartyPage(srn, index, disposalIndex)).get
        )

        recipientName = Option.when(howWasAssetDisposed == Sold)(
          List(
            updatedUserAnswers.get(IndividualNameOfAssetBuyerPage(srn, index, disposalIndex)),
            updatedUserAnswers.get(CompanyNameOfAssetBuyerPage(srn, index, disposalIndex)),
            updatedUserAnswers.get(PartnershipBuyerNamePage(srn, index, disposalIndex)),
            updatedUserAnswers
              .get(OtherBuyerDetailsPage(srn, index, disposalIndex))
              .map(_.name)
          ).flatten.head
        )

        recipientDetails = Option.when(howWasAssetDisposed == Sold)(
          List(
            updatedUserAnswers
              .get(AssetIndividualBuyerNiNumberPage(srn, index, disposalIndex))
              .flatMap(_.value.toOption.map(_.value)),
            updatedUserAnswers
              .get(AssetCompanyBuyerCrnPage(srn, index, disposalIndex))
              .flatMap(_.value.toOption.map(_.value)),
            updatedUserAnswers
              .get(PartnershipBuyerUtrPage(srn, index, disposalIndex))
              .flatMap(_.value.toOption.map(_.value)),
            updatedUserAnswers
              .get(OtherBuyerDetailsPage(srn, index, disposalIndex))
              .map(_.description)
          ).flatten.headOption
        )

        recipientReasonNoDetails = Option.when(howWasAssetDisposed == Sold)(
          List(
            updatedUserAnswers
              .get(AssetIndividualBuyerNiNumberPage(srn, index, disposalIndex))
              .flatMap(_.value.swap.toOption.map(_.value)),
            updatedUserAnswers
              .get(AssetCompanyBuyerCrnPage(srn, index, disposalIndex))
              .flatMap(_.value.swap.toOption.map(_.value)),
            updatedUserAnswers
              .get(PartnershipBuyerUtrPage(srn, index, disposalIndex))
              .flatMap(_.value.swap.toOption.map(_.value))
          ).flatten.headOption
        )
        schemeName = request.schemeDetails.schemeName

        disposalAmount = updatedUserAnswers
          .map(OtherAssetsDisposalProgress.all(srn, index))
          .count { case (_, progress) => progress.completed }

        isMaximumReached = disposalAmount >= maxDisposalPerOtherAsset

      } yield Ok(
        view(
          viewModel(
            ViewModelParameters(
              srn,
              index,
              disposalIndex,
              schemeName,
              howWasAssetDisposed,
              whenWasAssetSold,
              whatIsOtherAsset,
              assetDisposedType,
              assetDisposalBuyerConnectedParty,
              totalConsiderationSale,
              independentValuation,
              anyPartAssetStillHeld,
              recipientName,
              recipientDetails.flatten,
              recipientReasonNoDetails.flatten,
              mode
            ),
            viewOnlyUpdated = false, // flag is not displayed on this tier
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion,
            compilationOrSubmissionDate = updatedUserAnswers.get(CompilationOrSubmissionDatePage(srn)),
            isMaximumReached = isMaximumReached
          )
        )
      )
    ).merge

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- request.userAnswers
          .set(OtherAssetsDisposalProgress(srn, index.refined, disposalIndex.refined), SectionJourneyStatus.Completed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, index, disposalIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(OtherAssetsDisposalCYAPage(srn), mode, request.userAnswers)
          )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  disposalIndex: Max50,
  schemeName: String,
  howWasAssetDisposed: HowDisposed,
  whenWasAssetSold: Option[LocalDate],
  whatIsOtherAsset: String,
  assetDisposedType: Option[IdentityType],
  assetDisposalBuyerConnectedParty: Option[Boolean],
  totalConsiderationSale: Option[Money],
  independentValuation: Option[Boolean],
  anyPartAssetStillHeld: Boolean,
  recipientName: Option[String],
  recipientDetails: Option[String],
  recipientReasonNoDetails: Option[String],
  mode: Mode
)
object AssetDisposalCYAController {
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
        normal = "assetDisposalCYA.normal.title",
        check = "assetDisposalCYA.change.title",
        viewOnly = "assetDisposalCYA.viewOnly.title"
      ),
      heading = parameters.mode.fold(
        normal = "assetDisposalCYA.normal.heading",
        check = Message("assetDisposalCYA.change.heading"),
        viewOnly = Message("assetDisposalCYA.viewOnly.heading", parameters.whatIsOtherAsset)
      ),
      description = Some(ParagraphMessage("assetDisposalCYA.paragraph")),
      page = CheckYourAnswersViewModel
        .singleSection(
          rows(
            parameters.srn,
            parameters.index,
            parameters.disposalIndex,
            parameters.schemeName,
            parameters.howWasAssetDisposed,
            parameters.whenWasAssetSold,
            parameters.whatIsOtherAsset,
            parameters.assetDisposedType,
            parameters.recipientReasonNoDetails,
            parameters.assetDisposalBuyerConnectedParty,
            parameters.totalConsiderationSale,
            parameters.independentValuation,
            parameters.anyPartAssetStillHeld,
            parameters.recipientName,
            parameters.recipientDetails
          )
        )
        .withMarginBottom(Margin.Fixed60Bottom)
        .withInset(Option.when(isMaximumReached)(Message("assetDisposalCYA.inset.maximumReached"))),
      refresh = None,
      buttonText =
        parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
        .onSubmit(parameters.srn, parameters.index, parameters.disposalIndex, parameters.mode),
      optViewOnlyDetails = if (parameters.mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "assetDisposalCYA.viewOnly.title",
            heading = Message("assetDisposalCYA.viewOnly.heading", parameters.whatIsOtherAsset),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                  .onSubmitViewOnly(parameters.srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                  .onSubmit(parameters.srn, parameters.index, parameters.disposalIndex, parameters.mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def rows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    howWasAssetDisposed: HowDisposed,
    whenWasAssetSold: Option[LocalDate],
    whatIsOtherAsset: String,
    assetDisposedType: Option[IdentityType],
    recipientReasonNoDetails: Option[String],
    assetDisposalBuyerConnectedParty: Option[Boolean],
    totalConsiderationSale: Option[Money],
    independentValuation: Option[Boolean],
    anyPartAssetStillHeld: Boolean,
    recipientName: Option[String],
    recipientDetails: Option[String]
  ): List[CheckYourAnswersRowViewModel] =
    baseRows(srn, index, disposalIndex, howWasAssetDisposed, whatIsOtherAsset) ++
      (howWasAssetDisposed match {
        case Sold =>
          soldRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            whenWasAssetSold,
            assetDisposedType,
            recipientReasonNoDetails,
            assetDisposalBuyerConnectedParty,
            totalConsiderationSale,
            independentValuation,
            anyPartAssetStillHeld,
            recipientName.get,
            recipientDetails
          )

        case Transferred =>
          transferredRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            anyPartAssetStillHeld
          )

        case Other(details) =>
          otherRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            details,
            anyPartAssetStillHeld
          )
      })

  private def baseRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    howDisposed: HowDisposed,
    whatIsOtherAsset: String
  ): List[CheckYourAnswersRowViewModel] = {
    val howDisposedName = howDisposed match {
      case Sold => Sold.name
      case Transferred => Transferred.name
      case Other(_) => Other.name
    }

    List(
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.asset"),
        whatIsOtherAsset
      ),
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.assetDisposed"),
        howDisposedName
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("assetDisposalCYA.section1.assetDisposed.hidden")
      )
    )
  }

  private def soldRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    whenWasAssetSold: Option[LocalDate],
    assetDisposedType: Option[IdentityType],
    recipientReasonNoDetails: Option[String],
    assetDisposalBuyerConnectedParty: Option[Boolean],
    totalConsiderationSale: Option[Money],
    independentValuation: Option[Boolean],
    anyPartAssetStillHeld: Boolean,
    recipientName: String,
    recipientDetails: Option[String]
  ): List[CheckYourAnswersRowViewModel] = {

    val receivedLoan = assetDisposedType match {
      case Some(IdentityType.Individual) => "assetDisposalCYA.identityType.pageContent"
      case Some(IdentityType.UKCompany) => "assetDisposalCYA.identityType.pageContent1"
      case Some(IdentityType.UKPartnership) => "assetDisposalCYA.identityType.pageContent2"
      case Some(IdentityType.Other) => "assetDisposalCYA.identityType.pageContent3"
    }

    val recipientNameUrl = assetDisposedType match {
      case Some(IdentityType.Individual) =>
        controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case Some(IdentityType.UKCompany) =>
        controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case Some(IdentityType.UKPartnership) =>
        controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case Some(IdentityType.Other) =>
        controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
    }

    val (
      recipientDetailsKey,
      recipientDetailsUrl,
      recipientDetailsIdChangeHiddenKey,
      recipientDetailsNoIdChangeHiddenKey
    ): (Message, String, String, String) =
      assetDisposedType match {
        case Some(IdentityType.Individual) =>
          (
            Message("assetDisposalCYA.section1.recipientDetails.nino", recipientName),
            controllers.nonsipp.otherassetsdisposal.routes.AssetIndividualBuyerNiNumberController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "assetDisposalCYA.section1.recipientDetails.nino.hidden",
            "assetDisposalCYA.section1.recipientDetails.noNinoReason.hidden"
          )
        case Some(IdentityType.UKCompany) =>
          (
            Message("assetDisposalCYA.section1.recipientDetails.crn", recipientName),
            controllers.nonsipp.otherassetsdisposal.routes.AssetCompanyBuyerCrnController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "assetDisposalCYA.section1.recipientDetails.crn.hidden",
            "assetDisposalCYA.section1.recipientDetails.noCrnReason.hidden"
          )
        case Some(IdentityType.UKPartnership) =>
          (
            Message("assetDisposalCYA.section1.recipientDetails.utr", recipientName),
            controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerUtrController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "assetDisposalCYA.section1.recipientDetails.utr.hidden",
            "assetDisposalCYA.section1.recipientDetails.noUtrReason.hidden"
          )
        case Some(IdentityType.Other) =>
          (
            Message("assetDisposalCYA.section1.recipientDetails.other", recipientName),
            controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "assetDisposalCYA.section1.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = assetDisposedType match {
      case Some(IdentityType.Individual) =>
        Message("assetDisposalCYA.section1.recipientDetails.noNinoReason", recipientName) ->
          controllers.nonsipp.otherassetsdisposal.routes.AssetIndividualBuyerNiNumberController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case Some(IdentityType.UKCompany) =>
        Message("assetDisposalCYA.section1.recipientDetails.noCrnReason", recipientName) ->
          controllers.nonsipp.otherassetsdisposal.routes.AssetCompanyBuyerCrnController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case Some(IdentityType.UKPartnership) =>
        Message("assetDisposalCYA.section1.recipientDetails.noUtrReason", recipientName) ->
          controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerUtrController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case Some(IdentityType.Other) =>
        Message("assetDisposalCYA.section1.recipientDetails.other", recipientName) ->
          controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

    }

    List(
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.whenWasAssetSold"),
        whenWasAssetSold.get.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("assetDisposalCYA.section1.whenWasAssetSold.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.totalConsiderationSale"),
        s"£${totalConsiderationSale.get.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.TotalConsiderationSaleAssetController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          "assetDisposalCYA.section1.totalConsiderationSale.hidden"
        )
      ),
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.whoPurchasedTheAsset"),
        receivedLoan
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.TypeOfAssetBuyerController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("assetDisposalCYA.section1.whoPurchasedTheAsset.hidden")
      ),
      CheckYourAnswersRowViewModel("assetDisposalCYA.section1.recipientName", recipientName)
        .withAction(
          SummaryAction("site.change", recipientNameUrl)
            .withVisuallyHiddenContent("assetDisposalCYA.section1.recipientName.hidden")
        )
    ) :?+ recipientDetails.map(
      reason =>
        CheckYourAnswersRowViewModel(recipientDetailsKey, reason)
          .withAction(
            SummaryAction("site.change", recipientDetailsUrl)
              .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
          )
    ) :?+ recipientReasonNoDetails.map(
      noreason =>
        CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, noreason)
          .withAction(
            SummaryAction("site.change", recipientNoDetailsUrl)
              .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
          )
    ) :+
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.assetDisposalBuyerConnectedParty", recipientName),
        if (assetDisposalBuyerConnectedParty.get) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          Message("assetDisposalCYA.section1.assetDisposalBuyerConnectedParty.hidden", recipientName)
        )
      ) :+
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.DisposalIndependentValuation"),
        if (independentValuation.get) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.AssetSaleIndependentValuationController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          "assetDisposalCYA.section1.DisposalIndependentValuation.hidden"
        )
      ) :+
      CheckYourAnswersRowViewModel(
        Message(
          "assetDisposalCYA.section1.anyPartAssetStillHeld",
          schemeName
        ),
        if (anyPartAssetStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          Message("assetDisposalCYA.section1.anyPartAssetStillHeld.hidden", schemeName)
        )
      )
  }

  private def transferredRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    anyPartAssetStillHeld: Boolean
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message(
          "assetDisposalCYA.section1.anyPartAssetStillHeld",
          schemeName
        ),
        if (anyPartAssetStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          Message("assetDisposalCYA.section1.anyPartAssetStillHeld.hidden", schemeName)
        )
      )
    )

  private def otherRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    otherDetails: String,
    anyPartAssetStillHeld: Boolean
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("assetDisposalCYA.section1.assetDisposedDetails"),
        otherDetails
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent("assetDisposalCYA.section1.assetDisposedDetails.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message(
          "assetDisposalCYA.section1.anyPartAssetStillHeld",
          schemeName
        ),
        if (anyPartAssetStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          Message("assetDisposalCYA.section1.anyPartAssetStillHeld.hidden", schemeName)
        )
      )
    )
}
