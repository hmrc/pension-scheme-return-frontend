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

import viewmodels.implicits._
import play.api.mvc._
import pages.nonsipp.otherassetsheld._
import utils.ListUtils.ListOps
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.toShow
import uk.gov.hmrc.http.HeaderCarrier
import pages.nonsipp.common._
import viewmodels.{DisplayMessage, Margin}
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models._
import models.SchemeHoldAsset._
import viewmodels.DisplayMessage.{Heading2, Message}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

case class OtherAssetsViewModelParameters(
  srn: Srn,
  index: Max5000,
  schemeName: String,
  description: String,
  isTangibleMoveableProperty: Either[String, Boolean],
  whyHeld: SchemeHoldAsset,
  acquisitionOrContributionDate: Option[LocalDate],
  sellerIdentityType: Option[IdentityType],
  sellerName: Option[String],
  sellerDetails: Option[String],
  sellerReasonNoDetails: Option[String],
  isSellerConnectedParty: Option[Boolean],
  totalCost: Money,
  isIndependentValuation: Option[Boolean],
  totalIncome: Either[String, Money],
  mode: Mode
)

type OtherAssetsData = (
  parameters: OtherAssetsViewModelParameters,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object OtherAssetsCheckAnswersUtils extends PsrControllerHelpers with CheckAnswersUtils[Max5000, OtherAssetsData] {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(OtherAssetsHeldPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] =
    Some(Message("nonsipp.summary.otherAssets.heading"))

  override def subheading(data: OtherAssetsData): Option[DisplayMessage] =
    Some(Message("nonsipp.summary.otherAssets.subheading", data.parameters.description))

  def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max5000] =
    request.userAnswers
      .map(OtherAssetsProgress.all())
      .filter(_._2.completed)
      .keys
      .map(refineStringIndex[Max5000.Refined])
      .toList
      .flatten

  def summaryDataAsync(srn: Srn, index: Max5000, mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[
    Result,
    OtherAssetsData
  ]] = Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max5000, mode: Mode)(using request: DataRequest[AnyContent]): Either[
    Result,
    OtherAssetsData
  ] = {
    for {
      description <- request.userAnswers
        .get(WhatIsOtherAssetPage(srn, index))
        .getOrRecoverJourney

      isTangibleMoveableProperty = request.userAnswers
        .get(IsAssetTangibleMoveablePropertyPage(srn, index))
        .getOrIncomplete

      whyHeld <- request.userAnswers
        .get(WhyDoesSchemeHoldAssetsPage(srn, index))
        .getOrRecoverJourney

      acquisitionOrContributionDate = Option.when(whyHeld != Transfer)(
        request.userAnswers
          .get(WhenDidSchemeAcquireAssetsPage(srn, index))
          .get
      )

      sellerIdentityType = Option.when(whyHeld == Acquisition)(
        request.userAnswers
          .get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller))
          .get
      )

      sellerName = Option.when(whyHeld == Acquisition)(
        List(
          request.userAnswers.get(IndividualNameOfOtherAssetSellerPage(srn, index)),
          request.userAnswers.get(CompanyNameOfOtherAssetSellerPage(srn, index)),
          request.userAnswers.get(PartnershipOtherAssetSellerNamePage(srn, index)),
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller))
            .map(_.name)
        ).flatten.head
      )

      sellerDetails = Option.when(whyHeld == Acquisition)(
        List(
          request.userAnswers
            .get(OtherAssetIndividualSellerNINumberPage(srn, index))
            .flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.OtherAssetSeller))
            .flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.OtherAssetSeller))
            .flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller))
            .map(_.description)
        ).flatten.headOption
      )

      sellerReasonNoDetails = Option.when(whyHeld == Acquisition)(
        List(
          request.userAnswers
            .get(OtherAssetIndividualSellerNINumberPage(srn, index))
            .flatMap(_.value.swap.toOption),
          request.userAnswers
            .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.OtherAssetSeller))
            .flatMap(_.value.swap.toOption),
          request.userAnswers
            .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.OtherAssetSeller))
            .flatMap(_.value.swap.toOption)
        ).flatten.headOption
      )

      isSellerConnectedParty = Option.when(whyHeld == Acquisition)(
        request.userAnswers
          .get(OtherAssetSellerConnectedPartyPage(srn, index))
          .get
      )

      totalCost <- request.userAnswers
        .get(CostOfOtherAssetPage(srn, index))
        .getOrRecoverJourney

      isIndependentValuation = Option.when(whyHeld != Transfer)(
        request.userAnswers
          .get(IndependentValuationPage(srn, index))
          .get
      )

      totalIncome = request.userAnswers
        .get(IncomeFromAssetPage(srn, index))
        .getOrIncomplete

      schemeName = request.schemeDetails.schemeName
    } yield (
      parameters = OtherAssetsViewModelParameters(
        srn,
        index,
        schemeName,
        description,
        isTangibleMoveableProperty,
        whyHeld,
        acquisitionOrContributionDate,
        sellerIdentityType,
        sellerName,
        sellerDetails.flatten,
        sellerReasonNoDetails.flatten,
        isSellerConnectedParty,
        totalCost,
        isIndependentValuation,
        totalIncome,
        mode
      ),
      viewOnlyUpdated = false,
      optYear = request.year,
      optCurrentVersion = request.currentVersion,
      optPreviousVersion = request.previousVersion
    )
  }

  def viewModel(data: OtherAssetsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.parameters,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  def viewModel(
    parameters: OtherAssetsViewModelParameters,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = parameters.mode,
      title = parameters.mode.fold(
        normal = "otherAssets.cya.title",
        check = "otherAssets.change.title",
        viewOnly = "otherAssets.viewOnly.title"
      ),
      heading = parameters.mode.fold(
        normal = "otherAssets.cya.heading",
        check = "otherAssets.change.heading",
        viewOnly = "otherAssets.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(parameters)
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = parameters.mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue",
        viewOnly = "site.continue"
      ),
      onSubmit = controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
        .onSubmit(parameters.srn, parameters.index, parameters.mode),
      optViewOnlyDetails = if (parameters.mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "otherAssets.viewOnly.title",
            heading = Message("otherAssets.viewOnly.heading"),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onSubmitViewOnly(parameters.srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onSubmit(parameters.srn, parameters.index, parameters.mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(parameters: OtherAssetsViewModelParameters): List[CheckYourAnswersSection] = {
    val section1 = detailsOfTransactionSection(
      parameters.srn,
      parameters.index,
      parameters.schemeName,
      parameters.description,
      parameters.isTangibleMoveableProperty,
      parameters.whyHeld,
      parameters.acquisitionOrContributionDate,
      parameters.totalCost,
      parameters.isIndependentValuation,
      parameters.totalIncome
    )

    if (parameters.whyHeld == Acquisition) {
      section1 ++ detailsOfAcquisitionSection(
        parameters.srn,
        parameters.index,
        parameters.sellerIdentityType.get,
        parameters.sellerName.get,
        parameters.sellerDetails,
        parameters.sellerReasonNoDetails,
        parameters.isSellerConnectedParty.get
      )
    } else {
      section1
    }
  }

  private def detailsOfTransactionSection(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    description: String,
    isTangibleMoveableProperty: Either[String, Boolean],
    whyHeld: SchemeHoldAsset,
    acquisitionOrContributionDate: Option[LocalDate],
    totalCost: Money,
    isIndependentValuation: Option[Boolean],
    totalIncome: Either[String, Money]
  ): List[CheckYourAnswersSection] = {

    val whyHeldString = whyHeld match {
      case Acquisition => "otherAssets.cya.acquisition"
      case Contribution => "otherAssets.cya.contribution"
      case Transfer => "otherAssets.cya.transfer"
    }

    List(
      CheckYourAnswersSection(
        if (isTangibleMoveableProperty.isLeft || totalIncome.isLeft) {
          Some(Heading2.medium("otherAssetsCheckAndUpdate.section1.heading"))
        } else {
          Some(Heading2.medium("otherAssets.cya.section1.heading"))
        },
        List(
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section1.baseRow1.key"),
            description
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent("otherAssets.cya.section1.baseRow1.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section1.baseRow2.key"),
            isTangibleMoveableProperty match {
              case Left(value) => s"$value"
              case Right(value) => if (value) "site.yes" else "site.no"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent("otherAssets.cya.section1.baseRow2.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section1.baseRow3.key", schemeName),
            whyHeldString
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.WhyDoesSchemeHoldAssetsController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent(Message("otherAssets.cya.section1.baseRow3.hidden", schemeName))
          )
        ) :?+
          acquisitionOrContributionDate.map(date =>
            CheckYourAnswersRowViewModel(
              Message("otherAssets.cya.section1.conditionalRow1.key", schemeName),
              date.show
            ).withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, CheckMode)
                  .url
              ).withVisuallyHiddenContent(Message("otherAssets.cya.section1.conditionalRow1.hidden", schemeName))
            )
          ) :+
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section1.baseRow4.key"),
            s"£${totalCost.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent("otherAssets.cya.section1.baseRow4.hidden")
          ) :?+
          isIndependentValuation.map(isIndependentValuation =>
            CheckYourAnswersRowViewModel(
              Message("otherAssets.cya.section1.conditionalRow2.key"),
              if (isIndependentValuation) "site.yes" else "site.no"
            ).withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
                  .onPageLoad(srn, index, CheckMode)
                  .url
              ).withVisuallyHiddenContent("otherAssets.cya.section1.conditionalRow2.hidden")
            )
          ) :+
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section1.baseRow5.key"),
            totalIncome match {
              case Left(value) => s"$value"
              case Right(value) => s"£${value.displayAs}"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent("otherAssets.cya.section1.baseRow5.hidden")
          )
      )
    )
  }

  private def detailsOfAcquisitionSection(
    srn: Srn,
    index: Max5000,
    sellerIdentityType: IdentityType,
    sellerName: String,
    sellerDetails: Option[String],
    sellerReasonNoDetails: Option[String],
    isSellerConnectedParty: Boolean
  ): List[CheckYourAnswersSection] = {

    val sellerIdentityMessage = sellerIdentityType match {
      case IdentityType.Individual => "otherAssetSeller.identityType.pageContent"
      case IdentityType.UKCompany => "otherAssetSeller.identityType.pageContent1"
      case IdentityType.UKPartnership => "otherAssetSeller.identityType.pageContent2"
      case IdentityType.Other => "otherAssetSeller.identityType.pageContent3"
    }

    val sellerNameUrl = sellerIdentityType match {
      case IdentityType.Individual =>
        controllers.nonsipp.otherassetsheld.routes.IndividualNameOfOtherAssetSellerController
          .onPageLoad(srn, index, CheckMode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.otherassetsheld.routes.CompanyNameOfOtherAssetSellerController
          .onPageLoad(srn, index, CheckMode)
          .url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.otherassetsheld.routes.PartnershipNameOfOtherAssetsSellerController
          .onPageLoad(srn, index, CheckMode)
          .url
      case IdentityType.Other =>
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller)
          .url + "#sellerName"
    }

    val sellerDetailsUrl = sellerIdentityType match {
      case IdentityType.Individual =>
        controllers.nonsipp.otherassetsheld.routes.OtherAssetIndividualSellerNINumberController
          .onPageLoad(srn, index, CheckMode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller)
          .url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.common.routes.PartnershipRecipientUtrController
          .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller)
          .url
      case IdentityType.Other =>
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller)
          .url + "#sellerDetails"
    }

    val sellerDetailsType = sellerIdentityType match {
      case IdentityType.Individual => Message("otherAssets.cya.nino")
      case IdentityType.UKCompany => Message("otherAssets.cya.crn")
      case IdentityType.UKPartnership => Message("otherAssets.cya.utr")
      case IdentityType.Other => Message("otherAssets.cya.details")
    }

    val sellerNoDetailsHiddenText = sellerIdentityType match {
      case IdentityType.Other => Message("")
      case _ => Message("otherAssets.cya.section2.conditionalRow1.noDetails.hidden", sellerDetailsType)
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("otherAssets.cya.section2.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section2.baseRow1.key"),
            sellerIdentityMessage
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller)
                .url
            ).withVisuallyHiddenContent("otherAssets.cya.section2.baseRow1.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section2.baseRow2.key"),
            sellerName
          ).withAction(
            SummaryAction(
              "site.change",
              sellerNameUrl
            ).withVisuallyHiddenContent("otherAssets.cya.section2.baseRow2.hidden")
          )
        ) :?+ sellerDetails.map(details =>
          CheckYourAnswersRowViewModel(
            if (sellerIdentityType == IdentityType.Individual) {
              sellerDetailsType
            } else {
              Message("otherAssets.cya.section2.conditionalRow1.details.key", sellerName, sellerDetailsType)
            },
            details
          ).withAction(
            SummaryAction("site.change", sellerDetailsUrl)
              .withVisuallyHiddenContent(
                Message("otherAssets.cya.section2.conditionalRow1.details.hidden", sellerDetailsType)
              )
          )
        ) :?+ sellerReasonNoDetails.map(reasonNoDetails =>
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section2.conditionalRow1.noDetails.key", sellerDetailsType),
            reasonNoDetails
          ).withAction(
            SummaryAction("site.change", sellerDetailsUrl)
              .withVisuallyHiddenContent(sellerNoDetailsHiddenText)
          )
        ) :+
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section2.baseRow3.key"),
            if (isSellerConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent(Message("otherAssets.cya.section2.baseRow3.hidden"))
          )
      )
    )
  }

}
