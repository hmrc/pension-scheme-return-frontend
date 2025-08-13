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

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{AnyContent, Result, _}
import models.HowDisposed.{HowDisposed, _}
import utils.ListUtils.ListOps
import models.SchemeId.Srn
import cats.implicits.toShow
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{DisplayMessage, Margin}
import models.requests.DataRequest
import config.RefinedTypes.{Max50, Max5000}
import controllers.PsrControllerHelpers
import utils.IntUtils.toInt
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import config.Constants.maxLandOrPropertyDisposals
import pages.nonsipp.landorpropertydisposal._
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

type LandOrPropertyDisposalData = (
  parameters: LandOrPropertyDisposalViewModelParameters,
  srn: Srn,
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int],
  isMaximumReached: Boolean
)

case class LandOrPropertyDisposalViewModelParameters(
  srn: Srn,
  index: Max5000,
  disposalIndex: Max50,
  schemeName: String,
  howWasPropertyDisposed: HowDisposed,
  whenWasPropertySold: Option[LocalDate],
  addressLookUpPage: Address,
  landOrPropertyDisposedType: Option[IdentityType],
  landOrPropertyDisposalBuyerConnectedParty: Option[Boolean],
  totalProceedsSale: Option[Money],
  independentValuation: Option[Boolean],
  landOrPropertyStillHeld: Boolean,
  recipientName: Option[String],
  recipientDetails: Option[String],
  recipientReasonNoDetails: Option[String],
  mode: Mode
)

class LandOrPropertyDisposalCheckAnswersUtils(saveService: SaveService)
    extends PsrControllerHelpers
    with CheckAnswersUtils[(Max5000, Max50), LandOrPropertyDisposalData] {
  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(LandOrPropertyDisposalPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.landOrProperty.disposals.heading"))

  override def subheading(data: LandOrPropertyDisposalData): Option[DisplayMessage] =
    Some(
      Message(
        "nonsipp.summary.landOrProperty.disposals.subheading",
        data.parameters.disposalIndex.value,
        data.parameters.addressLookUpPage.addressLine1
      )
    )

  override def summaryDataAsync(srn: Srn, index: (Max5000, Max50), mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, LandOrPropertyDisposalData]] = {
    val (landOrPropertyIndex, disposalIndex) = index
    (for {
      updatedUserAnswers <- request.userAnswers
        .set(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndex, disposalIndex), SectionCompleted)
        .toOption
        .getOrRecoverJourneyT

      howWasPropertyDisposed <- updatedUserAnswers
        .get(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex))
        .getOrRecoverJourneyT

      addressLookUpPage <- updatedUserAnswers
        .get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex))
        .getOrRecoverJourneyT

      landOrPropertyStillHeld <- updatedUserAnswers
        .get(LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex))
        .getOrRecoverJourneyT

      totalProceedsSale = Option.when(howWasPropertyDisposed == Sold)(
        updatedUserAnswers.get(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex)).get
      )
      independentValuation = Option.when(howWasPropertyDisposed == Sold)(
        updatedUserAnswers.get(DisposalIndependentValuationPage(srn, landOrPropertyIndex, disposalIndex)).get
      )
      landOrPropertyDisposedType = Option.when(howWasPropertyDisposed == Sold)(
        updatedUserAnswers
          .get(WhoPurchasedLandOrPropertyPage(srn: Srn, landOrPropertyIndex, disposalIndex))
          .get
      )

      whenWasPropertySold = Option.when(howWasPropertyDisposed == Sold)(
        updatedUserAnswers.get(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex)).get
      )

      landOrPropertyDisposalBuyerConnectedParty = Option.when(howWasPropertyDisposed == Sold)(
        updatedUserAnswers
          .get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, landOrPropertyIndex, disposalIndex))
          .get
      )

      recipientName = Option.when(howWasPropertyDisposed == Sold)(
        List(
          updatedUserAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
          updatedUserAnswers.get(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
          updatedUserAnswers.get(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
          updatedUserAnswers
            .get(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex))
            .map(_.name)
        ).flatten.head
      )

      recipientDetails = Option.when(howWasPropertyDisposed == Sold)(
        List(
          updatedUserAnswers
            .get(IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.toOption.map(_.value)),
          updatedUserAnswers
            .get(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.toOption.map(_.value)),
          updatedUserAnswers
            .get(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.toOption.map(_.value)),
          updatedUserAnswers
            .get(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex))
            .map(_.description)
        ).flatten.headOption
      )

      recipientReasonNoDetails = Option.when(howWasPropertyDisposed == Sold)(
        List(
          updatedUserAnswers
            .get(IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.swap.toOption),
          updatedUserAnswers
            .get(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.swap.toOption),
          updatedUserAnswers
            .get(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex))
            .flatMap(_.value.swap.toOption)
        ).flatten.headOption
      )

      disposalAmount = updatedUserAnswers
        .map(LandPropertyDisposalCompleted.all(landOrPropertyIndex))
        .size

      schemeName = request.schemeDetails.schemeName

      _ <- saveService.save(updatedUserAnswers).liftF
      isMaximumReached = disposalAmount >= maxLandOrPropertyDisposals

    } yield (
      LandOrPropertyDisposalViewModelParameters(
        srn,
        landOrPropertyIndex,
        disposalIndex,
        schemeName,
        howWasPropertyDisposed,
        whenWasPropertySold,
        addressLookUpPage,
        landOrPropertyDisposedType,
        landOrPropertyDisposalBuyerConnectedParty,
        totalProceedsSale,
        independentValuation,
        landOrPropertyStillHeld,
        recipientName,
        recipientDetails.flatten,
        recipientReasonNoDetails.flatten,
        mode
      ),
      srn,
      mode,
      false, // flag is not displayed on this tier
      request.year,
      request.currentVersion,
      request.previousVersion,
      isMaximumReached
    )).value
  }

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[(Max5000, Max50)] =
    request.userAnswers
      .map(LandOrPropertyDisposalProgress.all())
      .flatMap { case (lpIndex, disposals) =>
        disposals.map { case (dIndex, progress) =>
          ((lpIndex, dIndex), progress)
        }
      }
      .keys
      .toList
      .sortBy((i, di) => s"$i $di")
      .flatMap((lpi, di) =>
        for {
          refinedLpi <- refineStringIndex[Max5000.Refined](lpi)
          refinedDi <- refineStringIndex[Max50.Refined](di)
        } yield (refinedLpi, refinedDi)
      )

  def viewModel(data: LandOrPropertyDisposalData): FormPageViewModel[CheckYourAnswersViewModel] =
    LandOrPropertyDisposalCheckAnswersUtils.viewModel(
      data.parameters,
      data.srn,
      data.mode,
      data.viewOnlyUpdated,
      data.optYear,
      data.optCurrentVersion,
      data.optPreviousVersion,
      data.isMaximumReached
    )
}

object LandOrPropertyDisposalCheckAnswersUtils {

  def apply(saveService: SaveService): LandOrPropertyDisposalCheckAnswersUtils =
    new LandOrPropertyDisposalCheckAnswersUtils(saveService)

  def viewModel(
    parameters: LandOrPropertyDisposalViewModelParameters,
    srn: Srn,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    isMaximumReached: Boolean
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = parameters.mode.fold(
        normal = "landPropertyDisposalCYA.normal.title",
        check = "landPropertyDisposalCYA.change.title",
        viewOnly = "landPropertyDisposalCYA.viewOnly.title"
      ),
      heading = parameters.mode.fold(
        normal = "landPropertyDisposalCYA.normal.heading",
        check = Message("landPropertyDisposalCYA.change.heading", parameters.addressLookUpPage.addressLine1),
        viewOnly = "landPropertyDisposalCYA.viewOnly.heading"
      ),
      description = Some(ParagraphMessage("landOrPropertyCYA.paragraph")),
      page = CheckYourAnswersViewModel
        .singleSection(
          rows(
            parameters.srn,
            parameters.index,
            parameters.disposalIndex,
            parameters.schemeName,
            parameters.howWasPropertyDisposed,
            parameters.whenWasPropertySold,
            parameters.addressLookUpPage,
            parameters.landOrPropertyDisposedType,
            parameters.recipientReasonNoDetails,
            parameters.landOrPropertyDisposalBuyerConnectedParty,
            parameters.totalProceedsSale,
            parameters.independentValuation,
            parameters.landOrPropertyStillHeld,
            parameters.recipientName,
            parameters.recipientDetails
          )
        )
        .withMarginBottom(Margin.Fixed60Bottom)
        .withInset(Option.when(isMaximumReached)(Message("landPropertyDisposalCYA.inset.maximumReached"))),
      refresh = None,
      buttonText =
        parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
        .onSubmit(parameters.srn, parameters.index, parameters.disposalIndex, parameters.mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "landPropertyDisposalCYA.viewOnly.title",
            heading = Message("landPropertyDisposalCYA.viewOnly.heading", parameters.addressLookUpPage.addressLine1),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
                  .onSubmit(srn, parameters.index, parameters.disposalIndex, mode)
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
    howWasPropertyDisposed: HowDisposed,
    whenWasPropertySold: Option[LocalDate],
    addressLookUpPage: Address,
    landOrPropertyDisposedType: Option[IdentityType],
    recipientReasonNoDetails: Option[String],
    landOrPropertyDisposalBuyerConnectedParty: Option[Boolean],
    totalProceedsSale: Option[Money],
    independentValuation: Option[Boolean],
    landOrPropertyStillHeld: Boolean,
    recipientName: Option[String],
    recipientDetails: Option[String]
  ): List[CheckYourAnswersRowViewModel] =
    baseRows(srn, index, disposalIndex, howWasPropertyDisposed, addressLookUpPage.addressLine1) ++
      (howWasPropertyDisposed match {
        case Sold =>
          soldRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            whenWasPropertySold,
            addressLookUpPage,
            landOrPropertyDisposedType,
            recipientReasonNoDetails,
            landOrPropertyDisposalBuyerConnectedParty,
            totalProceedsSale,
            independentValuation,
            landOrPropertyStillHeld,
            recipientName.get,
            recipientDetails
          )

        case Transferred =>
          transferredRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            addressLookUpPage,
            landOrPropertyStillHeld
          )

        case Other(details) =>
          otherRows(
            srn,
            index,
            disposalIndex,
            schemeName,
            details,
            addressLookUpPage,
            landOrPropertyStillHeld
          )
      })

  private def baseRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    howDisposed: HowDisposed,
    addressLine1: String
  ): List[CheckYourAnswersRowViewModel] = {
    val howDisposedName = howDisposed match {
      case Sold => Sold.name
      case Transferred => Transferred.name
      case Other(_) => Other.name
    }

    List(
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.propertyInUk", addressLine1),
        addressLine1
      ),
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.propertyDisposed", addressLine1),
        howDisposedName
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(("landPropertyDisposalCYA.section1.propertyDisposed.hidden", addressLine1))
      )
    )
  }

  private def soldRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    whenWasPropertySold: Option[LocalDate],
    addressLookUpPage: Address,
    landOrPropertyDisposedType: Option[IdentityType],
    recipientReasonNoDetails: Option[String],
    landOrPropertyDisposalBuyerConnectedParty: Option[Boolean],
    totalProceedsSale: Option[Money],
    independentValuation: Option[Boolean],
    landOrPropertyStillHeld: Boolean,
    recipientName: String,
    recipientDetails: Option[String]
  ): List[CheckYourAnswersRowViewModel] = {

    val receivedLoan = landOrPropertyDisposedType match {
      case Some(IdentityType.Individual) => "landOrPropertySeller.identityType.pageContent"
      case Some(IdentityType.UKCompany) => "landOrPropertySeller.identityType.pageContent1"
      case Some(IdentityType.UKPartnership) => "landOrPropertySeller.identityType.pageContent2"
      case _ => "landOrPropertySeller.identityType.pageContent3"
    }

    val recipientNameUrl = landOrPropertyDisposedType match {
      case Some(IdentityType.Individual) =>
        controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyIndividualBuyerNameController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case Some(IdentityType.UKCompany) =>
        controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerNameController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case Some(IdentityType.UKPartnership) =>
        controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerNameController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
      case _ =>
        controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
          .onSubmit(srn, index, disposalIndex, CheckMode)
          .url
    }

    val (
      recipientDetailsKey,
      recipientDetailsUrl,
      recipientDetailsIdChangeHiddenKey,
      recipientDetailsNoIdChangeHiddenKey
    ): (Message, String, String, String) =
      landOrPropertyDisposedType match {
        case Some(IdentityType.Individual) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.nino", recipientName),
            controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "landPropertyDisposalCYA.section1.recipientDetails.nino.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noNinoReason.hidden"
          )
        case Some(IdentityType.UKCompany) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.crn", recipientName),
            controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerCrnController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "landPropertyDisposalCYA.section1.recipientDetails.crn.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noCrnReason.hidden"
          )
        case Some(IdentityType.UKPartnership) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.utr", recipientName),
            controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "landPropertyDisposalCYA.section1.recipientDetails.utr.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noUtrReason.hidden"
          )
        case _ =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.other", recipientName),
            controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
              .onSubmit(srn, index, disposalIndex, CheckMode)
              .url,
            "landPropertyDisposalCYA.section1.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = landOrPropertyDisposedType match {
      case Some(IdentityType.Individual) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noNinoReason", recipientName) ->
          controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case Some(IdentityType.UKCompany) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noCrnReason", recipientName) ->
          controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerCrnController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case Some(IdentityType.UKPartnership) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noUtrReason", recipientName) ->
          controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

      case _ =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.other", recipientName) ->
          controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url

    }

    List(
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.whenWasPropertySold", addressLookUpPage.addressLine1),
        whenWasPropertySold.get.show
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.WhenWasPropertySoldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.whenWasPropertySold.hidden", addressLookUpPage.addressLine1)
        )
      ),
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.totalProceedsSale", addressLookUpPage.addressLine1),
        s"£${totalProceedsSale.get.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.TotalProceedsSaleLandPropertyController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.totalProceedsSaleInfo.hidden", addressLookUpPage.addressLine1)
        )
      ),
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.whoPurchasedLandOrProperty", addressLookUpPage.addressLine1),
        receivedLoan
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.WhoPurchasedLandOrPropertyController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.whoPurchasedLandOrProperty.hidden", addressLookUpPage.addressLine1)
        )
      ),
      CheckYourAnswersRowViewModel("landPropertyDisposalCYA.section1.recipientName", recipientName)
        .withAction(
          SummaryAction("site.change", recipientNameUrl)
            .withVisuallyHiddenContent("landPropertyDisposalCYA.section1.recipientName.hidden")
        )
    ) :?+ recipientDetails.map(reason =>
      CheckYourAnswersRowViewModel(recipientDetailsKey, reason)
        .withAction(
          SummaryAction("site.change", recipientDetailsUrl)
            .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
        )
    ) :?+ recipientReasonNoDetails.map(noreason =>
      CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, noreason)
        .withAction(
          SummaryAction("site.change", recipientNoDetailsUrl)
            .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
        )
    ) :+
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.landOrPropertyDisposalBuyerConnectedParty", recipientName),
        if (landOrPropertyDisposalBuyerConnectedParty.get) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.landOrPropertyDisposalBuyerConnectedPartyInfo.hidden", recipientName)
        )
      ) :+
      CheckYourAnswersRowViewModel(
        Message(
          "landPropertyDisposalCYA.section1.DisposalIndependentValuation",
          addressLookUpPage.addressLine1
        ),
        if (independentValuation.get) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.DisposalIndependentValuationInfo.hidden", addressLookUpPage.addressLine1)
        )
      ) :+
      CheckYourAnswersRowViewModel(
        Message(
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeld",
          addressLookUpPage.addressLine1,
          schemeName
        ),
        if (landOrPropertyStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden", addressLookUpPage.addressLine1)
        )
      )
  }

  private def transferredRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    addressLookUpPage: Address,
    landOrPropertyStillHeld: Boolean
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message(
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeld",
          addressLookUpPage.addressLine1,
          schemeName
        ),
        if (landOrPropertyStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden", addressLookUpPage.addressLine1)
        )
      )
    )

  private def otherRows(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    otherDetails: String,
    addressLookUpPage: Address,
    landOrPropertyStillHeld: Boolean
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.propertyDisposedDetails", addressLookUpPage.addressLine1),
        otherDetails
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.propertyDisposedDetails.hidden", addressLookUpPage.addressLine1)
        )
      ),
      CheckYourAnswersRowViewModel(
        Message(
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeld",
          addressLookUpPage.addressLine1,
          schemeName
        ),
        if (landOrPropertyStillHeld) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          ("landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden", addressLookUpPage.addressLine1)
        )
      )
    )
}
