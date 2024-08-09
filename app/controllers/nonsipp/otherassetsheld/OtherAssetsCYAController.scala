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

package controllers.nonsipp.otherassetsheld

import controllers.nonsipp.otherassetsheld.OtherAssetsCYAController._
import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import config.Refined.Max5000
import controllers.PSRController
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.common._
import models.requests.DataRequest
import pages.nonsipp.otherassetsheld._
import models.PointOfEntry.NoPointOfEntry
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import models.SchemeHoldAsset._
import play.api.i18n.MessagesApi
import viewmodels.Margin
import viewmodels.DisplayMessage.{Heading2, Message}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

class OtherAssetsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      //Clear any PointOfEntry
      saveService.save(
        request.userAnswers
          .set(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .getOrElse(request.userAnswers)
      )
      onPageLoadCommon(srn, index, mode)(implicitly)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)(implicitly)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    (
      for {

        description <- request.userAnswers
          .get(WhatIsOtherAssetPage(srn, index))
          .getOrRecoverJourney

        isTangibleMoveableProperty <- request.userAnswers
          .get(IsAssetTangibleMoveablePropertyPage(srn, index))
          .getOrRecoverJourney

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
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.OtherAssetSeller))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.OtherAssetSeller))
              .flatMap(_.value.swap.toOption.map(_.value))
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

        totalIncome <- request.userAnswers
          .get(IncomeFromAssetPage(srn, index))
          .getOrRecoverJourney

        schemeName = request.schemeDetails.schemeName

      } yield Ok(
        view(
          viewModel(
            ViewModelParameters(
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
            viewOnlyUpdated = false, // flag is not displayed on this tier
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion,
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
        )
      )
    ).merge

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers.set(OtherAssetsCompleted(srn, index), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetails(
            srn,
            fallbackCall =
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, mode)
          )
          .map {
            case None => controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(_) => navigator.nextPage(OtherAssetsCYAPage(srn), NormalMode, request.userAnswers)
          }
      } yield Redirect(redirectTo)
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  schemeName: String,
  description: String,
  isTangibleMoveableProperty: Boolean,
  whyHeld: SchemeHoldAsset,
  acquisitionOrContributionDate: Option[LocalDate],
  sellerIdentityType: Option[IdentityType],
  sellerName: Option[String],
  sellerDetails: Option[String],
  sellerReasonNoDetails: Option[String],
  isSellerConnectedParty: Option[Boolean],
  totalCost: Money,
  isIndependentValuation: Option[Boolean],
  totalIncome: Money,
  mode: Mode
)

object OtherAssetsCYAController {
  def viewModel(
    parameters: ViewModelParameters,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
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

  private def sections(parameters: ViewModelParameters): List[CheckYourAnswersSection] = {
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
    isTangibleMoveableProperty: Boolean,
    whyHeld: SchemeHoldAsset,
    acquisitionOrContributionDate: Option[LocalDate],
    totalCost: Money,
    isIndependentValuation: Option[Boolean],
    totalIncome: Money
  ): List[CheckYourAnswersSection] = {

    val whyHeldString = whyHeld match {
      case Acquisition => "otherAssets.cya.acquisition"
      case Contribution => "otherAssets.cya.contribution"
      case Transfer => "otherAssets.cya.transfer"
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("otherAssets.cya.section1.heading")),
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
            if (isTangibleMoveableProperty) "site.yes" else "site.no"
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
          acquisitionOrContributionDate.map(
            date =>
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
          isIndependentValuation.map(
            isIndependentValuation =>
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
            s"£${totalIncome.displayAs}"
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
        ) :?+ sellerDetails.map(
          details =>
            CheckYourAnswersRowViewModel(
              Message("otherAssets.cya.section2.conditionalRow1.details.key", sellerName, sellerDetailsType),
              details
            ).withAction(
              SummaryAction("site.change", sellerDetailsUrl)
                .withVisuallyHiddenContent(
                  Message("otherAssets.cya.section2.conditionalRow1.details.hidden", sellerDetailsType)
                )
            )
        ) :?+ sellerReasonNoDetails.map(
          reasonNoDetails =>
            CheckYourAnswersRowViewModel(
              Message("otherAssets.cya.section2.conditionalRow1.noDetails.key", sellerName, sellerDetailsType),
              reasonNoDetails
            ).withAction(
              SummaryAction("site.change", sellerDetailsUrl)
                .withVisuallyHiddenContent(sellerNoDetailsHiddenText)
            )
        ) :+
          CheckYourAnswersRowViewModel(
            Message("otherAssets.cya.section2.baseRow3.key", sellerName),
            if (isSellerConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                .onPageLoad(srn, index, CheckMode)
                .url
            ).withVisuallyHiddenContent(Message("otherAssets.cya.section2.baseRow3.hidden", sellerName))
          )
      )
    )
  }
}
