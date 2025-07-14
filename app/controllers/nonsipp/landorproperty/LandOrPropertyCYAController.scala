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

package controllers.nonsipp.landorproperty

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import models.SchemeHoldLandProperty.{Acquisition, Transfer}
import controllers.actions._
import navigation.Navigator
import models._
import pages.nonsipp.common._
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty._
import utils.nonsipp.LandOrPropertyCheckAnswersSectionUtils
import controllers.nonsipp.landorproperty.LandOrPropertyCYAController._
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class LandOrPropertyCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadCommon(srn: SchemeId.Srn, index: Max5000, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result = {
    request.userAnswers.get(LandOrPropertyProgress(srn, index)) match {
      case Some(value) if value.inProgress =>
        Redirect(
          controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, 1, mode)
        )
      case _ =>
        (
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
          } yield Ok(
            view(
              viewModel(
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
            )
          )
        ).merge
    }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(LandOrPropertyPrePopulated(srn, index))
      for {
        updatedAnswers <- request.userAnswers
          .setWhen(request.userAnswers.get(LandOrPropertyHeldPage(srn)).isEmpty)(LandOrPropertyHeldPage(srn), true)
          .setWhen(prePopulated.isDefined)(LandOrPropertyPrePopulated(srn, index), true)
          .mapK[Future]
        _ <- saveService.save(updatedAnswers)
        result <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall =
              controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onPageLoad(srn, index, mode)
          )
      } yield result.getOrRecoverJourney(_ =>
        Redirect(
          navigator
            .nextPage(LandOrPropertyCYAPage(srn), NormalMode, updatedAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object LandOrPropertyCYAController {
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
        LandOrPropertyCheckAnswersSectionUtils.landOrPropertySections(
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
      onSubmit = routes.LandOrPropertyCYAController.onSubmit(srn, index, mode),
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
                routes.LandOrPropertyCYAController.onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                routes.LandOrPropertyCYAController.onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

}
