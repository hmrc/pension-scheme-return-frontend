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

package controllers.nonsipp.landorpropertydisposal

import cats.implicits.toShow
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.LandPropertyDisposalCYAController._
import models.HowDisposed.{HowDisposed, Other, Sold, Transferred}
import models.SchemeId.Srn
import models._
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
import pages.nonsipp.landorpropertydisposal._
import play.api.i18n._
import play.api.mvc._
import services.{PsrSubmissionService, SaveService}
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandPropertyDisposalCYAController @Inject()(
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
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          howWasPropertyDisposed <- requiredPage(HowWasPropertyDisposedOfPage(srn, index, disposalIndex))
          addressLookUpPage <- requiredPage(LandOrPropertyAddressLookupPage(srn, index))
          landOrPropertyStillHeld <- requiredPage(LandOrPropertyStillHeldPage(srn, index, disposalIndex))

          totalProceedsSale = Option.when(howWasPropertyDisposed == Sold)(
            request.userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, index, disposalIndex)).get
          )
          independentValuation = Option.when(howWasPropertyDisposed == Sold)(
            request.userAnswers.get(DisposalIndependentValuationPage(srn, index, disposalIndex)).get
          )
          landOrPropertyDisposedType = Option.when(howWasPropertyDisposed == Sold)(
            request.userAnswers
              .get(WhoPurchasedLandOrPropertyPage(srn: Srn, index, disposalIndex))
              .get
          )

          whenWasPropertySold = Option.when(howWasPropertyDisposed == Sold)(
            request.userAnswers.get(WhenWasPropertySoldPage(srn, index, disposalIndex)).get
          )

          landOrPropertyDisposalSellerConnectedParty = Option.when(howWasPropertyDisposed == Sold)(
            request.userAnswers.get(LandOrPropertyDisposalSellerConnectedPartyPage(srn, index, disposalIndex)).get
          )

          recipientName = Option.when(howWasPropertyDisposed == Sold)(
            List(
              request.userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, index, disposalIndex)),
              request.userAnswers.get(CompanyBuyerNamePage(srn, index, disposalIndex)),
              request.userAnswers.get(PartnershipBuyerNamePage(srn, index, disposalIndex)),
              request.userAnswers
                .get(OtherBuyerDetailsPage(srn, index, disposalIndex))
                .map(_.name)
            ).flatten.head
          )

          recipientDetails = Option.when(howWasPropertyDisposed == Sold)(
            List(
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, index, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, index, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, index, disposalIndex))
                .flatMap(_.value.toOption.map(_.value)),
              request.userAnswers
                .get(OtherBuyerDetailsPage(srn, index, disposalIndex))
                .map(_.description)
            ).flatten.headOption
          )

          recipientReasonNoDetails = Option.when(howWasPropertyDisposed == Sold)(
            List(
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, index, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, index, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, index, disposalIndex))
                .flatMap(_.value.swap.toOption.map(_.value))
            ).flatten.headOption
          )
          schemeName = request.schemeDetails.schemeName

        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed,
                whenWasPropertySold,
                addressLookUpPage,
                landOrPropertyDisposedType,
                landOrPropertyDisposalSellerConnectedParty,
                totalProceedsSale,
                independentValuation,
                landOrPropertyStillHeld,
                recipientName,
                recipientDetails.flatten,
                recipientReasonNoDetails.flatten,
                mode
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers.set(LandPropertyDisposalCompletedPage(srn, index, disposalIndex), SectionCompleted)
        )
        _ <- saveService.save(updatedUserAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetails(srn)(implicitly, implicitly, request = DataRequest(request.request, updatedUserAnswers))
          .map {
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case Some(_) =>
              Redirect(
                navigator
                  .nextPage(
                    LandPropertyDisposalCompletedPage(srn, index, disposalIndex),
                    NormalMode,
                    updatedUserAnswers
                  )
              )
          }
      } yield redirectTo
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  disposalIndex: Max50,
  schemeName: String,
  howWasPropertyDisposed: HowDisposed,
  whenWasPropertySold: Option[LocalDate],
  addressLookUpPage: Address,
  landOrPropertyDisposedType: Option[IdentityType],
  landOrPropertyDisposalSellerConnectedParty: Option[Boolean],
  totalProceedsSale: Option[Money],
  independentValuation: Option[Boolean],
  landOrPropertyStillHeld: Boolean,
  recipientName: Option[String],
  recipientDetails: Option[String],
  recipientReasonNoDetails: Option[String],
  mode: Mode
)
object LandPropertyDisposalCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode.fold(
        normal = "checkYourAnswers.title",
        check = "landPropertyDisposalCYA.change.title"
      ),
      heading = parameters.mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("landPropertyDisposalCYA.change.heading", parameters.addressLookUpPage.addressLine1)
      ),
      description = Some(ParagraphMessage("landOrPropertyCYA.paragraph")),
      page = CheckYourAnswersViewModel.singleSection(
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
          parameters.landOrPropertyDisposalSellerConnectedParty,
          parameters.totalProceedsSale,
          parameters.independentValuation,
          parameters.landOrPropertyStillHeld,
          parameters.recipientName,
          parameters.recipientDetails
        )
      ),
      refresh = None,
      buttonText = parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue"),
      onSubmit = routes.LandPropertyDisposalCYAController
        .onSubmit(parameters.srn, parameters.index, parameters.disposalIndex, parameters.mode)
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
    landOrPropertyDisposalSellerConnectedParty: Option[Boolean],
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
            landOrPropertyDisposalSellerConnectedParty,
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
        ).withVisuallyHiddenContent("landPropertyDisposalCYA.section1.propertyDisposed.hidden")
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
    landOrPropertyDisposalSellerConnectedParty: Option[Boolean],
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
      case Some(IdentityType.Other) => "landOrPropertySeller.identityType.pageContent3"
    }

    val recipientNameUrl = landOrPropertyDisposedType match {
      case Some(IdentityType.Individual) =>
        routes.LandOrPropertyIndividualBuyerNameController.onSubmit(srn, index, disposalIndex, CheckMode).url
      case Some(IdentityType.UKCompany) =>
        routes.CompanyBuyerNameController.onSubmit(srn, index, disposalIndex, CheckMode).url
      case Some(IdentityType.UKPartnership) =>
        routes.PartnershipBuyerNameController.onSubmit(srn, index, disposalIndex, CheckMode).url
      case Some(IdentityType.Other) =>
        routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, CheckMode).url
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
            routes.IndividualBuyerNinoNumberController.onSubmit(srn, index, disposalIndex, CheckMode).url,
            "landPropertyDisposalCYA.section1.recipientDetails.nino.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noNinoReason.hidden"
          )
        case Some(IdentityType.UKCompany) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.crn", recipientName),
            routes.CompanyBuyerCrnController.onSubmit(srn, index, disposalIndex, CheckMode).url,
            "landPropertyDisposalCYA.section1.recipientDetails.crn.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noCrnReason.hidden"
          )
        case Some(IdentityType.UKPartnership) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.utr", recipientName),
            routes.PartnershipBuyerUtrController.onSubmit(srn, index, disposalIndex, CheckMode).url,
            "landPropertyDisposalCYA.section1.recipientDetails.utr.hidden",
            "landPropertyDisposalCYA.section1.recipientDetails.noUtrReason.hidden"
          )
        case Some(IdentityType.Other) =>
          (
            Message("landPropertyDisposalCYA.section1.recipientDetails.other", recipientName),
            routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, CheckMode).url,
            "landPropertyDisposalCYA.section1.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = landOrPropertyDisposedType match {
      case Some(IdentityType.Individual) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noNinoReason", recipientName) ->
          routes.IndividualBuyerNinoNumberController.onSubmit(srn, index, disposalIndex, CheckMode).url

      case Some(IdentityType.UKCompany) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noCrnReason", recipientName) ->
          routes.CompanyBuyerCrnController.onSubmit(srn, index, disposalIndex, CheckMode).url

      case Some(IdentityType.UKPartnership) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.noUtrReason", recipientName) ->
          routes.PartnershipBuyerUtrController.onSubmit(srn, index, disposalIndex, CheckMode).url

      case Some(IdentityType.Other) =>
        Message("landPropertyDisposalCYA.section1.recipientDetails.other", recipientName) ->
          routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, CheckMode).url

    }

    List(
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.whenWasPropertySold", addressLookUpPage.addressLine1),
        whenWasPropertySold.get.show
      ).withAction(
        SummaryAction(
          "site.change",
          routes.WhenWasPropertySoldController.onSubmit(srn, index, disposalIndex, CheckMode).url
        ).withVisuallyHiddenContent("landPropertyDisposalCYA.section1.whenWasPropertySold.hidden")
      ),
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.totalProceedsSale", addressLookUpPage.addressLine1),
        s"£${totalProceedsSale.get.displayAs}"
      ).withAction(
        SummaryAction(
          "site.change",
          routes.TotalProceedsSaleLandPropertyController.onSubmit(srn, index, disposalIndex, CheckMode).url
        ).withVisuallyHiddenContent(
          "landPropertyDisposalCYA.section1.totalProceedsSaleInfo.hidden"
        )
      ),
      CheckYourAnswersRowViewModel(
        Message("landPropertyDisposalCYA.section1.whoPurchasedLandOrProperty", addressLookUpPage.addressLine1),
        receivedLoan
      ).withAction(
        SummaryAction(
          "site.change",
          routes.WhoPurchasedLandOrPropertyController.onSubmit(srn, index, disposalIndex, CheckMode).url
        ).withVisuallyHiddenContent("landPropertyDisposalCYA.section1.whoPurchasedLandOrProperty.hidden")
      ),
      CheckYourAnswersRowViewModel("landPropertyDisposalCYA.section1.recipientName", recipientName)
        .withAction(
          SummaryAction("site.change", recipientNameUrl)
            .withVisuallyHiddenContent("landPropertyDisposalCYA.section1.recipientName.hidden")
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
        Message("landPropertyDisposalCYA.section1.landOrPropertyDisposalSellerConnectedParty", recipientName),
        if (landOrPropertyDisposalSellerConnectedParty.get) "site.yes" else "site.no"
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalSellerConnectedPartyController
            .onSubmit(srn, index, disposalIndex, CheckMode)
            .url
        ).withVisuallyHiddenContent(
          "landPropertyDisposalCYA.section1.landOrPropertyDisposalSellerConnectedPartyInfo.hidden"
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
          "landPropertyDisposalCYA.section1.DisposalIndependentValuationInfo.hidden"
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
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden"
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
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden"
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
        ).withVisuallyHiddenContent("landPropertyDisposalCYA.section1.propertyDisposedDetails.hidden")
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
          "landPropertyDisposalCYA.section1.landOrPropertyStillHeldInfo.hidden"
        )
      )
    )
}
