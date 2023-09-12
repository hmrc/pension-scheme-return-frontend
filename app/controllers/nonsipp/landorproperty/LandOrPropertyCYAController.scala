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

package controllers.nonsipp.landorproperty

import cats.implicits.toShow
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorproperty.LandOrPropertyCYAController._
import models.SchemeHoldLandProperty.{Acquisition, Transfer}
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.common._
import pages.nonsipp.landorproperty._
import play.api.i18n._
import play.api.mvc._
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}

class LandOrPropertyCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    checkOrChange: CheckOrChange
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          landOrPropertyInUk <- requiredPage(LandPropertyInUKPage(srn, index))
          landRegistryTitleNumber <- requiredPage(LandRegistryTitleNumberPage(srn, index))
          addressLookUpPage <- requiredPage(LandOrPropertyAddressLookupPage(srn, index))
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
            val landOrPropertyLeaseDetailsPage = request.userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).get
            val leaseConnectedParty = request.userAnswers.get(IsLesseeConnectedPartyPage(srn, index)).get

            Tuple4(
              landOrPropertyLeaseDetailsPage._1,
              landOrPropertyLeaseDetailsPage._2,
              landOrPropertyLeaseDetailsPage._3,
              leaseConnectedParty
            )
          }

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
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
                checkOrChange
              )
            )
          )
        )
      ).merge

    }

  def onSubmit(srn: Srn, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage(LandOrPropertyCYAPage(srn), NormalMode, request.userAnswers))
    }
}

case class ViewModelParameters(
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
  checkOrChange: CheckOrChange
)
object LandOrPropertyCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange.fold(check = "checkYourAnswers.title", change = "landOrPropertyCYA.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "checkYourAnswers.heading",
        change = Message("landOrPropertyCYA.change.heading")
      ),
      description = Some(ParagraphMessage("landOrPropertyCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.landOrPropertyInUk,
          parameters.landRegistryTitleNumber,
          parameters.holdLandProperty,
          parameters.landOrPropertyAcquire,
          parameters.landOrPropertyTotalCost,
          parameters.receivedLandType,
          parameters.recipientName,
          parameters.recipientDetails,
          parameters.recipientReasonNoDetails,
          parameters.landOrPropertySellerConnectedParty,
          parameters.landPropertyIndependentValuation,
          parameters.leaseDetails,
          parameters.landOrPropertyResidential,
          parameters.landOrPropertyLease,
          parameters.landOrPropertyTotalIncome,
          parameters.addressLookUpPage,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = routes.LandOrPropertyCYAController.onSubmit(parameters.srn, parameters.checkOrChange)
    )

  private def sections(
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
      case Acquisition =>
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
        ) ++ DetailsOfTheAcquisition(
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
                addressLookUpPage.addressLine1,
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
            .map(
              leaseDetails =>
                leaseDetailsSection(
                  srn,
                  index,
                  leaseDetails._1,
                  leaseDetails._2,
                  leaseDetails._3,
                  leaseDetails._4,
                  addressLookUpPage.addressLine1,
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
              routes.LandPropertyInUKController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section1.landOrPropertyInUk.hidden")
          ),
          CheckYourAnswersRowViewModel(
            "landOrPropertyCYA.section1.address",
            address.addressLine1
          ).withAction(
            SummaryAction(
              "site.change",
              routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index).url
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
              routes.LandRegistryTitleNumberController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section1.landRegistryTitleNumber.hidden")
          ),
          landRegistryTitleNumber.value match {
            case Right(titleNumber) =>
              CheckYourAnswersRowViewModel(
                Message("landOrPropertyCYA.section1.registryTitleNumber.yes"),
                titleNumber
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.LandRegistryTitleNumberController.onPageLoad(srn, index, mode).url
                ).withVisuallyHiddenContent("landOrPropertyCYA.section1.landRegistryTitleNumber.yes.hidden")
              )
            case Left(reason) =>
              CheckYourAnswersRowViewModel(
                Message("landOrPropertyCYA.section1.registryTitleNumber.no", address.addressLine1),
                reason
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.LandRegistryTitleNumberController.onPageLoad(srn, index, mode).url
                ).withVisuallyHiddenContent("landOrPropertyCYA.section1.landRegistryTitleNumber.no.hidden")
              )
          }
        )
      )
    )

  private def DetailsOfTheAcquisition(
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
        routes.LandPropertyIndividualSellersNameController.onPageLoad(srn, index, mode).url
      case IdentityType.UKCompany => routes.CompanySellerNameController.onPageLoad(srn, index, mode).url
      case IdentityType.UKPartnership => routes.PartnershipSellerNameController.onPageLoad(srn, index, mode).url
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
            routes.IndividualSellerNiController.onPageLoad(srn, index, mode).url,
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
          routes.IndividualSellerNiController.onPageLoad(srn, index, mode).url
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
              ).withVisuallyHiddenContent("landOrPropertyCYA.section3.whoReceivedLand.hidden")
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
              routes.LandOrPropertySellerConnectedPartyController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section3.landOrPropertySellerConnectedPartyInfo.hidden")
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
        Some(Heading2.medium("landOrPropertyCYA.section2.heading", address)),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section2.holdProperty", schemeName, address),
            holdLandProperty.name
          ).withAction(
            SummaryAction(
              "site.change",
              routes.WhyDoesSchemeHoldLandPropertyController.onPageLoad(srn, index, mode).url + "#holdLandProperty"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section2.holdLandProperty.hidden")
          )
        )
          ++
            List(
              optLandOrPropertyAcquire.map(
                landOrPropertyAcquire =>
                  CheckYourAnswersRowViewModel(
                    Message("landOrPropertyCYA.section2.acquire", address),
                    s"${landOrPropertyAcquire.show}"
                  ).withAction(
                    SummaryAction(
                      "site.change",
                      routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, mode).url + "#landOrPropertyAcquire"
                    ).withVisuallyHiddenContent("landOrPropertyCYA.section2.landOrPropertyAcquire.hidden")
                  )
              )
            ).flatten ++
          List(
            CheckYourAnswersRowViewModel(
              Message("landOrPropertyCYA.section2.totalCost", address),
              landOrPropertyTotalCost.displayAs
            ).withAction(
              SummaryAction(
                "site.change",
                routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("landOrPropertyCYA.section2.landOrPropertyTotalCost.hidden")
            )
          ) ++
          List(
            optLandPropertyIndependentValuation.map(
              landPropertyIndependentValuation =>
                CheckYourAnswersRowViewModel(
                  Message("landOrPropertyCYA.section2.independentValuation", address),
                  if (landPropertyIndependentValuation) "site.yes" else "site.no"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    routes.LandPropertyIndependentValuationController.onPageLoad(srn, index, mode).url
                  ).withVisuallyHiddenContent("landOrPropertyCYA.section2.landPropertyIndependentValuation.hidden")
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
        Some(Heading2.medium("landOrPropertyCYA.section4.heading", address)),
        List(
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.residential", address),
            if (landOrPropertyResidential) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section4.landOrPropertyResidential.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.propertyLease", address),
            if (landOrPropertyLease) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IsLandPropertyLeasedController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section4.landOrPropertyLease.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section4.propertyTotalIncome", address),
            s"£${landOrPropertyTotalIncome.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.LandOrPropertyTotalIncomeController.onPageLoad(srn, index, mode).url + "#landOrPropertyTotalIncome"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section4.landOrPropertyTotalIncome.hidden")
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
    address: String,
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
              routes.LandOrPropertyLeaseDetailsController.onPageLoad(srn, index, mode).url + "#leaseName"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section5.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section5.leaseValue", leaseValue.displayAs),
            s"£${leaseValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.LandOrPropertyLeaseDetailsController.onPageLoad(srn, index, mode).url + "#leaseValue"
            ).withVisuallyHiddenContent("landOrPropertyCYA.section5.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel("landOrPropertyCYA.section5.leaseDate", leaseDate.show)
            .withAction(
              SummaryAction(
                "site.change",
                routes.LandOrPropertyLeaseDetailsController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("landOrPropertyCYA.section5.leaseDate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("landOrPropertyCYA.section5.connectedParty", address),
            if (leaseConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IsLesseeConnectedPartyController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("landOrPropertyCYA.section5.leaseConnected.hidden")
          )
        )
      )
    )
}
