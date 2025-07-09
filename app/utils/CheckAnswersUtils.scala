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

package utils

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import play.api.mvc._
import config.RefinedTypes.{Max3, Max5000}
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsCompletedOrUpdated
import pages.nonsipp.landorproperty._
import controllers.nonsipp.BasicDetailsCheckYourAnswersController
import models._
import play.api.i18n.Messages
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}
import play.api.mvc.Results.Redirect
import viewmodels.implicits._
import queries.Gettable
import cats.data.NonEmptyList
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import play.api.libs.json.Reads
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.common._
import models.requests.DataRequest
import controllers.nonsipp.landorproperty.LandOrPropertyCYAController

import java.time.LocalDateTime

object CheckAnswersUtils {

  // Copy from PSRController
  private def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[?]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  // Copy from PSRController
  def loggedInUserNameOrRedirect(implicit request: DataRequest[?]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  // Moved from BasicDetailsController
  def buildBasicDetailsViewModel(
    srn: Srn,
    mode: Mode,
    showBackLink: Boolean,
    eitherJourneyNavigationResultOrRecovery: Either[Result, Boolean],
    periods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    currentUserAnswers: UserAnswers,
    compilationOrSubmissionDate: Option[LocalDateTime] // added here as parameter
  )(implicit
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, FormPageViewModel[CheckYourAnswersViewModel]] =
    for {
      schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
      activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
      whyNoBankAccount = currentUserAnswers.get(WhyNoBankAccountPage(srn))
      whichTaxYearPage = currentUserAnswers.get(WhichTaxYearPage(srn))
      userName <- loggedInUserNameOrRedirect
      journeyByPassed <- eitherJourneyNavigationResultOrRecovery
    } yield BasicDetailsCheckYourAnswersController.viewModel(
      srn,
      mode,
      schemeMemberNumbers,
      activeBankAccount,
      whyNoBankAccount,
      whichTaxYearPage,
      periods,
      userName,
      request.schemeDetails,
      request.pensionSchemeId,
      request.pensionSchemeId.isPSP,
      viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getBasicDetailsCompletedOrUpdated(currentUserAnswers, request.previousUserAnswers.get) == Updated
      } else {
        false
      },
      optYear = request.year,
      optCurrentVersion = request.currentVersion,
      optPreviousVersion = request.previousVersion,
      compilationOrSubmissionDate = compilationOrSubmissionDate,
      journeyByPassed = journeyByPassed,
      showBackLink = showBackLink
    )

  def buildLandOrPropertyViewModel(srn: Srn, index: Max5000, mode: Mode)(implicit
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, FormPageViewModel[CheckYourAnswersViewModel]] =
    for {
      landOrPropertyInUk <- requiredPage(LandPropertyInUKPage(srn, index))
      landRegistryTitleNumber <- requiredPage(LandRegistryTitleNumberPage(srn, index))
      addressLookUpPage <- requiredPage(LandOrPropertyChosenAddressPage(srn, index))
      holdLandProperty <- requiredPage(WhyDoesSchemeHoldLandPropertyPage(srn, index))
      landOrPropertyTotalCost <- requiredPage(LandOrPropertyTotalCostPage(srn, index))

      landPropertyIndependentValuation = Option.when(holdLandProperty != SchemeHoldLandProperty.Transfer)(
        request.userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).get
      )
      landOrPropertyAcquire = Option.when(holdLandProperty != SchemeHoldLandProperty.Transfer)(
        request.userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)).get
      )

      receivedLandType = Option.when(holdLandProperty == SchemeHoldLandProperty.Acquisition)(
        request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)).get
      )

      landOrPropertySellerConnectedParty = Option.when(holdLandProperty == SchemeHoldLandProperty.Acquisition)(
        request.userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, index)).get
      )

      recipientName = Option.when(holdLandProperty == SchemeHoldLandProperty.Acquisition)(
        List(
          request.userAnswers.get(LandPropertyIndividualSellersNamePage(srn, index)),
          request.userAnswers.get(CompanySellerNamePage(srn, index)),
          request.userAnswers.get(PartnershipSellerNamePage(srn, index)),
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .map(_.name)
        ).flatten.head
      )

      recipientDetails = Option.when(holdLandProperty == SchemeHoldLandProperty.Acquisition)(
        List(
          request.userAnswers.get(IndividualSellerNiPage(srn, index)).flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.toOption.map(_.value)),
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .map(_.description)
        ).flatten.headOption
      )

      recipientReasonNoDetails = Option.when(holdLandProperty == SchemeHoldLandProperty.Acquisition)(
        List(
          request.userAnswers
            .get(IndividualSellerNiPage(srn, index))
            .flatMap(_.value.swap.toOption.map(_.value)),
          request.userAnswers
            .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.swap.toOption.map(_.value)),
          request.userAnswers
            .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.swap.toOption.map(_.value))
        ).flatten.headOption
      )

      landOrPropertyResidential <- requiredPage(IsLandOrPropertyResidentialPage(srn, index))
      landOrPropertyLease <- requiredPage(IsLandPropertyLeasedPage(srn, index))
      landOrPropertyTotalIncome <- requiredPage(LandOrPropertyTotalIncomePage(srn, index))

      leaseDetails = Option.when(landOrPropertyLease) {
        val landOrPropertyLeaseDetailsPage =
          request.userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).get
        val leaseConnectedParty = request.userAnswers.get(IsLesseeConnectedPartyPage(srn, index)).get

        Tuple4(
          landOrPropertyLeaseDetailsPage._1,
          landOrPropertyLeaseDetailsPage._2,
          landOrPropertyLeaseDetailsPage._3,
          leaseConnectedParty
        )
      }

      schemeName = request.schemeDetails.schemeName
    } yield LandOrPropertyCYAController.viewModel(
      srn,
      index,
      schemeName,
      landOrPropertyInUk,
      landRegistryTitleNumber,
      holdLandProperty,
      landOrPropertyAcquire,
      landOrPropertyTotalCost,
      landPropertyIndependentValuation,
      receivedLandType,
      recipientName,
      recipientDetails.flatten,
      recipientReasonNoDetails.flatten,
      landOrPropertySellerConnectedParty,
      landOrPropertyResidential,
      landOrPropertyLease,
      landOrPropertyTotalIncome,
      addressLookUpPage,
      leaseDetails,
      mode,
      viewOnlyUpdated = false,
      optYear = request.year,
      optCurrentVersion = request.currentVersion,
      optPreviousVersion = request.previousVersion
    )
}
