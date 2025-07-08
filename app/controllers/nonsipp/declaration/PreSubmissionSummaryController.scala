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

package controllers.nonsipp.declaration

import services.{PsrRetrievalService, PsrVersionsService, SchemeDateService}
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import play.api.mvc.{MessagesControllerComponents, _}
import pages.nonsipp.landorproperty._
import controllers.nonsipp.BasicDetailsCheckYourAnswersController
import controllers.actions.IdentifyAndRequireData
import utils.nonsipp.SchemeDetailNavigationUtils
import models.{IdentitySubject, NormalMode, SchemeHoldLandProperty}
import pages.nonsipp.common._
import play.api.i18n.{I18nSupport, MessagesApi, _}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.SummaryView
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import navigation.Navigator
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersSection, CheckYourAnswersViewModel, FormPageViewModel}
import models.requests.DataRequest
import controllers.nonsipp.landorproperty.LandOrPropertyCYAController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PreSubmissionSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    Future.successful(viewModel(srn).map(vm => Ok(view(vm, request.schemeDetails.schemeName))).merge)
  }

  def basicDetails(srn: Srn)(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, List[CheckYourAnswersSection]] = for {
    schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
    activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
    whyNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
    whichTaxYearPage = request.userAnswers.get(WhichTaxYearPage(srn))
    userName <- loggedInUserNameOrRedirect
    //      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
    periods <- schemeDateService.taxYearOrAccountingPeriods(srn).getOrRecoverJourney
  } yield BasicDetailsCheckYourAnswersController.sections(
    srn,
    NormalMode,
    activeBankAccount,
    whyNoBankAccount,
    whichTaxYearPage,
    periods,
    schemeMemberNumbers,
    userName,
    request.schemeDetails,
    request.pensionSchemeId,
    request.pensionSchemeId.isPSP
  )

//  def employerContributions(srn: Srn, memberIndex: Max300)(using
//    request: DataRequest[AnyContent],
//    messages: Messages
//  ) = for {
//    membersName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
//    indexes <- ???
//    employerCYAs <- indexes.map(secondaryIndex => EmployerContributionsCYAController.buildCYA(srn, index, secondaryIndex)).sequence
//  } yield CheckYourAnswersViewModel(
//    EmployerContributionsCYAController.rows(srn, NormalMode, memberIndex, membersName.fullName, employerCYAs),
//    inset = if (employerCYAs.length == 50) Some("employerContributionsCYA.inset") else None,
//    paginatedViewModel = Some(
//      PaginatedViewModel(
//        Message(
//          "employerContributions.MemberList.pagination.label",
//          pagination.pageStart,
//          pagination.pageEnd,
//          pagination.totalSize
//        ),
//        pagination
//      )
//    )
//  )

  def landOrProperties(
    srn: Srn
  )(using request: DataRequest[AnyContent], messages: Messages): Either[Result, List[CheckYourAnswersSection]] =
    val indexes = request.userAnswers
      .map(LandOrPropertyProgress.all())
      .filter(_._2.completed)
      .keys
      .map(refineStringIndex[Max5000.Refined](_))
      .collect { case Some(i) => i }

    indexes.toList
      .map { index =>
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
        } yield LandOrPropertyCYAController.sections(
          srn,
          index,
          request.schemeDetails.schemeName,
          landOrPropertyInUk,
          landRegistryTitleNumber,
          holdLandProperty,
          landOrPropertyAcquire,
          landOrPropertyTotalCost,
          receivedLandType,
          recipientName,
          recipientDetails.flatten,
          recipientReasonNoDetails.flatten,
          landOrPropertySellerConnectedParty,
          landPropertyIndependentValuation,
          leaseDetails,
          landOrPropertyResidential,
          landOrPropertyLease,
          landOrPropertyTotalIncome,
          addressLookUpPage,
          NormalMode
        )
      }
      .reduce((a, b) =>
        for {
          aRight <- a
          bRight <- b
        } yield aRight ++ bRight
      )

  def viewModel(
    srn: Srn
  )(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, FormPageViewModel[CheckYourAnswersViewModel]] =
    for {
      basicDetailsSections <- basicDetails(srn)
      landOrPropertySections <- landOrProperties(srn)
      allSections = basicDetailsSections ++ landOrPropertySections
      noActionsSections = allSections.map(x => x.copy(rows = x.rows.map(_.copy(actions = Nil))))
    } yield FormPageViewModel[CheckYourAnswersViewModel](
      Message("nonsipp.summary.title"),
      Message("nonsipp.summary.heading"),
      CheckYourAnswersViewModel(
        noActionsSections
      ),
      routes.PsaDeclarationController.onPageLoad(srn)
    )
}
