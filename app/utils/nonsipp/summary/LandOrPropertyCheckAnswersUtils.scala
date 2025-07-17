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
import play.api.mvc.{AnyContent, Result}
import utils.ListUtils.ListOps
import models.SchemeHoldLandProperty._
import models.SchemeId.Srn
import cats.implicits.toShow
import pages.nonsipp.common._
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PsrControllerHelpers
import utils.IntUtils.given
import pages.nonsipp.landorproperty._
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import java.time.LocalDate

object LandOrPropertyCheckAnswersUtils extends PsrControllerHelpers {

  def landOrPropertySummaryData(srn: Srn, index: Max5000, mode: Mode)(using request: DataRequest[AnyContent]): Either[
    Result,
    (
      srn: Srn,
      index: Max5000,
      schemeName: String,
      landOrPropertyInUk: Boolean,
      landRegistryTitleNumber: ConditionalYesNo[String, String],
      holdLandProperty: SchemeHoldLandProperty,
      landOrPropertyAcquire: Option[LocalDate],
      landOrPropertyTotalCost: Money,
      landPropertyIndependentValuation: Option[Boolean],
      receivedLandType: Option[IdentityType],
      recipientName: Option[String],
      recipientDetails: Option[String],
      recipientReasonNoDetails: Option[String],
      landOrPropertySellerConnectedParty: Option[Boolean],
      landOrPropertyResidential: Boolean,
      landOrPropertyLease: Boolean,
      landOrPropertyTotalIncome: Money,
      addressLookUpPage: Address,
      leaseDetails: Option[(String, Money, LocalDate, Boolean)],
      mode: Mode,
      viewOnlyUpdated: Boolean,
      optYear: Option[String],
      optCurrentVersion: Option[Int],
      optPreviousVersion: Option[Int]
    )
  ] = {
    for {
      landOrPropertyInUk <- requiredPage(LandPropertyInUKPage(srn, index))
      landRegistryTitleNumber <- requiredPage(LandRegistryTitleNumberPage(srn, index))
      addressLookUpPage <- requiredPage(LandOrPropertyChosenAddressPage(srn, index))
      holdLandProperty <- requiredPage(WhyDoesSchemeHoldLandPropertyPage(srn, index))
      landOrPropertyTotalCost <- requiredPage(LandOrPropertyTotalCostPage(srn, index))

      landPropertyIndependentValuation = Option.when(holdLandProperty != Transfer)(
        request.userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).get
      )
      landOrPropertyAcquire = Option.when(holdLandProperty != Transfer)(
        request.userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)).get
      )

      receivedLandType = Option.when(holdLandProperty == Acquisition)(
        request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)).get
      )

      landOrPropertySellerConnectedParty = Option.when(holdLandProperty == Acquisition)(
        request.userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, index)).get
      )

      recipientName = Option.when(holdLandProperty == Acquisition)(
        List(
          request.userAnswers.get(LandPropertyIndividualSellersNamePage(srn, index)),
          request.userAnswers.get(CompanySellerNamePage(srn, index)),
          request.userAnswers.get(PartnershipSellerNamePage(srn, index)),
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .map(_.name)
        ).flatten.head
      )

      recipientDetails = Option.when(holdLandProperty == Acquisition)(
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

      recipientReasonNoDetails = Option.when(holdLandProperty == Acquisition)(
        List(
          request.userAnswers
            .get(IndividualSellerNiPage(srn, index))
            .flatMap(_.value.swap.toOption),
          request.userAnswers
            .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.swap.toOption),
          request.userAnswers
            .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller))
            .flatMap(_.value.swap.toOption)
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
    } yield (
      srn = srn,
      index = index,
      schemeName = schemeName,
      landOrPropertyInUk = landOrPropertyInUk,
      landRegistryTitleNumber = landRegistryTitleNumber,
      holdLandProperty = holdLandProperty,
      landOrPropertyAcquire = landOrPropertyAcquire,
      landOrPropertyTotalCost = landOrPropertyTotalCost,
      landPropertyIndependentValuation = landPropertyIndependentValuation,
      receivedLandType = receivedLandType,
      recipientName = recipientName,
      recipientDetails = recipientDetails.flatten,
      recipientReasonNoDetails = recipientReasonNoDetails.flatten,
      landOrPropertySellerConnectedParty = landOrPropertySellerConnectedParty,
      landOrPropertyResidential = landOrPropertyResidential,
      landOrPropertyLease = landOrPropertyLease,
      landOrPropertyTotalIncome = landOrPropertyTotalIncome,
      addressLookUpPage = addressLookUpPage,
      leaseDetails = leaseDetails,
      mode = mode,
      viewOnlyUpdated = false,
      optYear = request.year,
      optCurrentVersion = request.currentVersion,
      optPreviousVersion = request.previousVersion
    )
  }

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    landOrPropertyInUk: Boolean,
    landRegistryTitleNumber: ConditionalYesNo[String, String],
    holdLandProperty: SchemeHoldLandProperty,
    landOrPropertyAcquire: Option[LocalDate],
    landOrPropertyTotalCost: Money,
    landPropertyIndependentValuation: Option[Boolean],
    receivedLandType: Option[IdentityType],
    recipientName: Option[String],
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    landOrPropertySellerConnectedParty: Option[Boolean],
    landOrPropertyResidential: Boolean,
    landOrPropertyLease: Boolean,
    landOrPropertyTotalIncome: Money,
    addressLookUpPage: Address,
    leaseDetails: Option[(String, Money, LocalDate, Boolean)],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "landOrPropertyCYA.normal.title",
        check = "landOrPropertyCYA.change.title",
        viewOnly = "landOrPropertyCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "landOrPropertyCYA.normal.heading",
        check = Message("landOrPropertyCYA.change.heading", addressLookUpPage.addressLine1),
        viewOnly = Message("landOrPropertyCYA.viewOnly.heading", addressLookUpPage.addressLine1)
      ),
      description = Some(ParagraphMessage("landOrPropertyCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          schemeName,
          landOrPropertyInUk,
          landRegistryTitleNumber,
          holdLandProperty,
          landOrPropertyAcquire,
          landOrPropertyTotalCost,
          receivedLandType,
          recipientName,
          recipientDetails,
          recipientReasonNoDetails,
          landOrPropertySellerConnectedParty,
          landPropertyIndependentValuation,
          leaseDetails,
          landOrPropertyResidential,
          landOrPropertyLease,
          landOrPropertyTotalIncome,
          addressLookUpPage,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "landOrPropertyCYA.viewOnly.title",
            heading = Message("landOrPropertyCYA.viewOnly.heading", addressLookUpPage.addressLine1),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  def sections(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    landOrPropertyInUk: Boolean,
    landRegistryTitleNumber: ConditionalYesNo[String, String],
    holdLandProperty: SchemeHoldLandProperty,
    landOrPropertyAcquire: Option[LocalDate],
    landOrPropertyTotalCost: Money,
    receivedLandType: Option[IdentityType],
    recipientName: Option[String],
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    landOrPropertySellerConnectedParty: Option[Boolean],
    landPropertyIndependentValuation: Option[Boolean],
    leaseDetails: Option[(String, Money, LocalDate, Boolean)],
    landOrPropertyResidential: Boolean,
    landOrPropertyLease: Boolean,
    landOrPropertyTotalIncome: Money,
    addressLookUpPage: Address,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    holdLandProperty match {
      case SchemeHoldLandProperty.Acquisition =>
        locationOfTheLandOrProperty(
          srn,
          index,
          landOrPropertyInUk,
          landRegistryTitleNumber,
          addressLookUpPage,
          mode
        ) ++ detailsOfTheTransaction(
          srn,
          index,
          schemeName,
          holdLandProperty,
          landOrPropertyAcquire,
          landOrPropertyTotalCost,
          landPropertyIndependentValuation,
          addressLookUpPage.addressLine1,
          mode
        ) ++ detailsOfTheAcquisition(
          srn,
          index,
          receivedLandType.get,
          recipientName.get,
          recipientDetails,
          recipientReasonNoDetails,
          landOrPropertySellerConnectedParty.get,
          addressLookUpPage.addressLine1,
          mode
        ) ++
          leaseDetailsAndIncome(
            srn,
            index,
            landOrPropertyResidential,
            landOrPropertyLease,
            landOrPropertyTotalIncome,
            addressLookUpPage.addressLine1,
            mode
          ) ++
          leaseDetails
            .map { leaseDetails =>
              leaseDetailsSection(
                srn,
                index,
                leaseDetails._1,
                leaseDetails._2,
                leaseDetails._3,
                leaseDetails._4,
                mode
              )
            }
            .getOrElse(List.empty)

      case _ =>
        locationOfTheLandOrProperty(
          srn,
          index,
          landOrPropertyInUk,
          landRegistryTitleNumber,
          addressLookUpPage,
          mode
        ) ++ detailsOfTheTransaction(
          srn,
          index,
          schemeName,
          holdLandProperty,
          landOrPropertyAcquire,
          landOrPropertyTotalCost,
          landPropertyIndependentValuation,
          addressLookUpPage.addressLine1,
          mode
        ) ++ leaseDetailsAndIncome(
          srn,
          index,
          landOrPropertyResidential,
          landOrPropertyLease,
          landOrPropertyTotalIncome,
          addressLookUpPage.addressLine1,
          mode
        ) ++
          leaseDetails
            .map(leaseDetails =>
              leaseDetailsSection(
                srn,
                index,
                leaseDetails._1,
                leaseDetails._2,
                leaseDetails._3,
                leaseDetails._4,
                mode
              )
            )
            .getOrElse(List.empty)
    }

  private def locationOfTheLandOrProperty(
    srn: Srn,
    index: Max5000,
    landOrPropertyInUk: Boolean,
    landRegistryTitleNumber: ConditionalYesNo[String, String],
    address: Address,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("landOrPropertyCYA.section1.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section1.propertyInUk", address.addressLine1),
            if (landOrPropertyInUk) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section1.landOrPropertyInUk.hidden")
          ),
          CheckYourAnswersRowViewModel(
            "landOrPropertyCYA.section1.address",
            ListMessage(address.asNel.map(Message(_)), ListType.NewLine)
          ).withAction(
            SummaryAction(
              "site.change",
              if (address.isManualAddress) {
                controllers.nonsipp.landorproperty.routes.LandPropertyAddressManualController
                  .onPageLoad(srn, index, isUkAddress = landOrPropertyInUk, mode)
                  .url
              } else {
                controllers.nonsipp.landorproperty.routes.LandOrPropertyPostcodeLookupController
                  .onPageLoad(srn, index, mode)
                  .url
              }
            ).withVisuallyHiddenContent("landOrPropertyCYA.section1.addressInfo.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section1.isThereATitleNumber", address.addressLine1),
            landRegistryTitleNumber.value match {
              case Right(_) => "site.yes"
              case Left(_) => "site.no"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
                .onPageLoad(srn, index, mode)
                .url + "#registryTitleQuestion"
            ).withVisuallyHiddenContent(
              ("landOrPropertyCYA.section1.landRegistryTitleNumber.hidden", address.addressLine1)
            )
          ),
          landRegistryTitleNumber.value match {
            case Right(titleNumber) =>
              CheckYourAnswersRowViewModel(
                Message("landOrPropertyCYA.section1.registryTitleNumber.yes"),
                titleNumber.toUpperCase
              ).withAction(
                SummaryAction(
                  "site.change",
                  controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
                    .onPageLoad(srn, index, mode)
                    .url + "#registryTitleValue"
                ).withVisuallyHiddenContent(
                  ("landOrPropertyCYA.section1.landRegistryTitleNumber.yes.hidden", address.addressLine1)
                )
              )
            case Left(reason) =>
              CheckYourAnswersRowViewModel(
                Message("landOrPropertyCYA.section1.registryTitleNumber.no", address.addressLine1),
                reason
              ).withAction(
                SummaryAction(
                  "site.change",
                  controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
                    .onPageLoad(srn, index, mode)
                    .url + "#registryTitleValue"
                ).withVisuallyHiddenContent(
                  ("landOrPropertyCYA.section1.landRegistryTitleNumber.no.hidden", address.addressLine1)
                )
              )
          }
        )
      )
    )

  private def detailsOfTheAcquisition(
    srn: Srn,
    index: Max5000,
    receivedLandType: IdentityType,
    recipientName: String,
    optRecipientDetails: Option[String],
    optRecipientReasonNoDetails: Option[String],
    landOrPropertySellerConnectedParty: Boolean,
    address: String,
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val receivedLoan = receivedLandType match {
      case IdentityType.Individual => "landOrPropertySeller.identityType.pageContent"
      case IdentityType.UKCompany => "landOrPropertySeller.identityType.pageContent1"
      case IdentityType.UKPartnership => "landOrPropertySeller.identityType.pageContent2"
      case IdentityType.Other => "landOrPropertySeller.identityType.pageContent3"
    }

    val recipientNameUrl = receivedLandType match {
      case IdentityType.Individual =>
        controllers.nonsipp.landorproperty.routes.LandPropertyIndividualSellersNameController
          .onPageLoad(srn, index, mode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.landorproperty.routes.CompanySellerNameController.onPageLoad(srn, index, mode).url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.landorproperty.routes.PartnershipSellerNameController.onPageLoad(srn, index, mode).url
      case IdentityType.Other =>
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
          .url
    }

    val (
      recipientDetailsKey,
      recipientDetailsUrl,
      recipientDetailsIdChangeHiddenKey,
      recipientDetailsNoIdChangeHiddenKey
    ): (Message, String, String, String) =
      receivedLandType match {
        case IdentityType.Individual =>
          (
            Message("landOrPropertyCYA.section3.recipientDetails.nino", recipientName),
            controllers.nonsipp.landorproperty.routes.IndividualSellerNiController.onPageLoad(srn, index, mode).url,
            "landOrPropertyCYA.section3.recipientDetails.nino.hidden",
            "landOrPropertyCYA.section3.recipientDetails.noNinoReason.hidden"
          )
        case IdentityType.UKCompany =>
          (
            Message("landOrPropertyCYA.section3.recipientDetails.crn", recipientName),
            controllers.nonsipp.common.routes.CompanyRecipientCrnController
              .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
              .url,
            "landOrPropertyCYA.section3.recipientDetails.crn.hidden",
            "landOrPropertyCYA.section3.recipientDetails.noCrnReason.hidden"
          )
        case IdentityType.UKPartnership =>
          (
            Message("landOrPropertyCYA.section3.recipientDetails.utr", recipientName),
            controllers.nonsipp.common.routes.PartnershipRecipientUtrController
              .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
              .url,
            "landOrPropertyCYA.section3.recipientDetails.utr.hidden",
            "landOrPropertyCYA.section3.recipientDetails.noUtrReason.hidden"
          )
        case IdentityType.Other =>
          (
            Message("landOrPropertyCYA.section3.recipientDetails.other", recipientName),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController
              .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
              .url,
            "landOrPropertyCYA.section3.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = receivedLandType match {
      case IdentityType.Individual =>
        Message("landOrPropertyCYA.section3.recipientDetails.noNinoReason", recipientName) ->
          controllers.nonsipp.landorproperty.routes.IndividualSellerNiController.onPageLoad(srn, index, mode).url
      case IdentityType.UKCompany =>
        Message("landOrPropertyCYA.section3.recipientDetails.noCrnReason", recipientName) ->
          controllers.nonsipp.common.routes.CompanyRecipientCrnController
            .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
            .url
      case IdentityType.UKPartnership =>
        Message("landOrPropertyCYA.section3.recipientDetails.noUtrReason", recipientName) ->
          controllers.nonsipp.common.routes.PartnershipRecipientUtrController
            .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
            .url
      case IdentityType.Other =>
        Message("landOrPropertyCYA.section3.recipientDetails.other", recipientName) ->
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
            .url
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("landOrPropertyCYA.section3.heading")),
        List(
          CheckYourAnswersRowViewModel(Message("landOrPropertyCYA.section3.whoReceivedLand", address), receivedLoan)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, mode, IdentitySubject.LandOrPropertySeller)
                  .url
              ).withVisuallyHiddenContent(("landOrPropertyCYA.section3.whoReceivedLand.hidden", address))
            ),
          CheckYourAnswersRowViewModel("landOrPropertyCYA.section3.recipientName", recipientName)
            .withAction(
              SummaryAction("site.change", recipientNameUrl)
                .withVisuallyHiddenContent("landOrPropertyCYA.section3.recipientName.hidden")
            )
        ) :?+ optRecipientDetails.map { recipientDetails =>
          CheckYourAnswersRowViewModel(recipientDetailsKey, recipientDetails)
            .withAction(
              SummaryAction("site.change", recipientDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
            )
        } :?+ optRecipientReasonNoDetails.map { recipientReasonNoDetails =>
          CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, recipientReasonNoDetails)
            .withAction(
              SummaryAction("site.change", recipientNoDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
            )
        } :+
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section3.landOrPropertySellerConnectedParty", recipientName),
            if (landOrPropertySellerConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
                .onPageLoad(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(
              ("landOrPropertyCYA.section3.landOrPropertySellerConnectedPartyInfo.hidden", recipientName)
            )
          )
      )
    )
  }

  private def detailsOfTheTransaction(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    holdLandProperty: SchemeHoldLandProperty,
    optLandOrPropertyAcquire: Option[LocalDate],
    landOrPropertyTotalCost: Money,
    optLandPropertyIndependentValuation: Option[Boolean],
    address: String,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium(("landOrPropertyCYA.section2.heading", address))),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section2.holdProperty", schemeName, address),
            holdLandProperty.name
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.WhyDoesSchemeHoldLandPropertyController
                .onPageLoad(srn, index, mode)
                .url + "#holdLandProperty"
            ).withVisuallyHiddenContent(
              Message("landOrPropertyCYA.section2.holdLandProperty.hidden", schemeName, address)
            )
          )
        )
          ++
            List(
              optLandOrPropertyAcquire.map(landOrPropertyAcquire =>
                CheckYourAnswersRowViewModel(
                  Message("landOrPropertyCYA.section2.acquire", address),
                  s"${landOrPropertyAcquire.show}"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController
                      .onPageLoad(srn, index, mode)
                      .url + "#landOrPropertyAcquire"
                  ).withVisuallyHiddenContent(("landOrPropertyCYA.section2.landOrPropertyAcquire.hidden", address))
                )
              )
            ).flatten ++
            List(
              CheckYourAnswersRowViewModel(
                Message("landOrPropertyCYA.section2.totalCost", address),
                s"£${landOrPropertyTotalCost.displayAs}"
              ).withAction(
                SummaryAction(
                  "site.change",
                  controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController
                    .onPageLoad(srn, index, mode)
                    .url
                ).withVisuallyHiddenContent(("landOrPropertyCYA.section2.landOrPropertyTotalCost.hidden", address))
              )
            ) ++
            List(
              optLandPropertyIndependentValuation.map(landPropertyIndependentValuation =>
                CheckYourAnswersRowViewModel(
                  Message("landOrPropertyCYA.section2.independentValuation", address),
                  if (landPropertyIndependentValuation) "site.yes" else "site.no"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
                      .onPageLoad(srn, index, mode)
                      .url
                  ).withVisuallyHiddenContent(
                    ("landOrPropertyCYA.section2.landPropertyIndependentValuation.hidden", address)
                  )
                )
              )
            ).flatten
      )
    )

  private def leaseDetailsAndIncome(
    srn: Srn,
    index: Max5000,
    landOrPropertyResidential: Boolean,
    landOrPropertyLease: Boolean,
    landOrPropertyTotalIncome: Money,
    address: String,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium(("landOrPropertyCYA.section4.heading", address))),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.residential", address),
            if (landOrPropertyResidential) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.IsLandOrPropertyResidentialController
                .onPageLoad(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(("landOrPropertyCYA.section4.landOrPropertyResidential.hidden", address))
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.propertyLease", address),
            if (landOrPropertyLease) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.IsLandPropertyLeasedController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent(("landOrPropertyCYA.section4.landOrPropertyLease.hidden", address))
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.propertyTotalIncome", address),
            s"£${landOrPropertyTotalIncome.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController
                .onPageLoad(srn, index, mode)
                .url + "#landOrPropertyTotalIncome"
            ).withVisuallyHiddenContent(("landOrPropertyCYA.section4.landOrPropertyTotalIncome.hidden", address))
          )
        )
      )
    )

  private def leaseDetailsSection(
    srn: Srn,
    index: Max5000,
    leaseName: String,
    leaseValue: Money,
    leaseDate: LocalDate,
    leaseConnectedParty: Boolean,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("landOrPropertyCYA.section5.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section5.leaseName", leaseName.show),
            s"${leaseName.show}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
                .onPageLoad(srn, index, mode)
                .url + "#leaseName"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section5.leaseName.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section5.leaseValue", leaseValue.displayAs),
            s"£${leaseValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
                .onPageLoad(srn, index, mode)
                .url + "#leaseValue"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section5.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel("landOrPropertyCYA.section5.leaseDate", leaseDate.show)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
                  .onPageLoad(srn, index, mode)
                  .url
              ).withVisuallyHiddenContent("landOrPropertyCYA.section5.leaseDate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section5.connectedParty", leaseName.show),
            if (leaseConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.landorproperty.routes.IsLesseeConnectedPartyController
                .onPageLoad(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(("landOrPropertyCYA.section5.leaseConnected.hidden", leaseName.show))
          )
        )
      )
    )
}
