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

package controllers.nonsipp.shares

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import models.ConditionalYesNo._
import utils.nonsipp.summary.SharesCheckAnswersUtils
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.given
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.common._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import pages.nonsipp.shares._
import play.api.mvc.Call
import play.api.inject.bind
import models.SchemeHoldShare._
import views.html.CheckYourAnswersView
import models.TypeOfShares._

import scala.concurrent.Future

class SharesCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockSaveService: SaveService = mock[SaveService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[SaveService].toInstance(mockSaveService)
  )

  override protected def beforeAll(): Unit = {
    reset(mockSchemeDateService)
    reset(mockPsrSubmissionService)
  }

  private val index = 1
  private val taxYear = Some(Left(dateRange))
  private val subject = IdentitySubject.SharesSeller

  private def onPageLoad(mode: Mode): Call =
    routes.SharesCYAController.onPageLoad(srn, index, mode)
  private def onSubmit(mode: Mode) = routes.SharesCYAController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.SharesCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.SharesCYAController.onSubmitViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val filledUserAnswersContribution = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), ConnectedParty)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Contribution)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, index), companyName)
    .unsafeSet(HowManySharesPage(srn, index), totalShares)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index), companyName)
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index), false)
    .unsafeSet(CostOfSharesPage(srn, index), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index), true)
    .unsafeSet(TotalAssetValuePage(srn, index), money)
    .unsafeSet(SharesTotalIncomePage(srn, index), money)

  private val filledUserAnswersAcquisitionIndividual = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), SponsoringEmployer)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.no[String, Crn](noCrnReason))
    .unsafeSet(ClassOfSharesPage(srn, index), companyName)
    .unsafeSet(HowManySharesPage(srn, index), totalShares)
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
    .unsafeSet(IndividualNameOfSharesSellerPage(srn, index), individualName)
    .unsafeSet(SharesIndividualSellerNINumberPage(srn, index), conditionalYesNoNino)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index), false)
    .unsafeSet(CostOfSharesPage(srn, index), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index), true)
    .unsafeSet(TotalAssetValuePage(srn, index), money)
    .unsafeSet(SharesTotalIncomePage(srn, index), money)

  private val filledUserAnswersAcquisitionCompany = filledUserAnswersAcquisitionIndividual
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
    .unsafeSet(CompanyNameOfSharesSellerPage(srn, index), companyName)
    .unsafeSet(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller), conditionalYesNoCrn)

  private val filledUserAnswersAcquisitionPartnership = filledUserAnswersAcquisitionIndividual
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKPartnership)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index), partnershipName)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller), conditionalYesNoUtr)

  private val filledUserAnswersAcquisitionOther = filledUserAnswersAcquisitionIndividual
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Other)
    .unsafeSet(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller), otherRecipientDetails)

  private val filledUserAnswersTransfer = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), Unquoted)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Transfer)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, index), companyName)
    .unsafeSet(HowManySharesPage(srn, index), totalShares)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
    .unsafeSet(CostOfSharesPage(srn, index), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index), false)
    .unsafeSet(TotalAssetValuePage(srn, index), money)
    .unsafeSet(SharesTotalIncomePage(srn, index), money)

  private val incompleteUserAnswers = filledUserAnswersContribution
    .unsafeSet(
      SharesProgress(srn, index),
      SectionJourneyStatus.InProgress(
        controllers.nonsipp.shares.routes.CostOfSharesController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    )

  "SharesCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswersContribution) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            SharesCheckAnswersUtils.viewModel(
              srn,
              index,
              schemeName,
              typeOfShare = ConnectedParty,
              holdShares = Contribution,
              whenDidSchemeAcquire = Some(localDate),
              companyNameRelatedShares = companyName,
              companySharesCrn = ConditionalYesNo.yes[String, Crn](crn),
              companyName,
              howManyShares = totalShares,
              identityType = Some(IdentityType.UKPartnership),
              recipientName = Some(companyName),
              recipientDetails = None,
              recipientReasonNoDetails = None,
              sharesFromConnectedParty = None,
              costOfShares = money,
              shareIndependentValue = true,
              totalAssetValue = None,
              sharesTotalIncome = money,
              mode = mode,
              viewOnlyUpdated = true
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct Contribution ${mode.toString} view")
      )

      List(
        (filledUserAnswersAcquisitionIndividual, IdentityType.Individual, individualName, nino.value),
        (filledUserAnswersAcquisitionCompany, IdentityType.UKCompany, companyName, crn.crn),
        (filledUserAnswersAcquisitionPartnership, IdentityType.UKPartnership, partnershipName, utr.value),
        (filledUserAnswersAcquisitionOther, IdentityType.Other, otherRecipientName, otherRecipientDescription)
      ).foreach { (filledAnswers, sellerType, sellerName, sellerDetails) =>
        act.like(
          renderView(onPageLoad(mode), filledAnswers) { implicit app => implicit request =>
            injected[CheckYourAnswersView].apply(
              SharesCheckAnswersUtils.viewModel(
                srn,
                index,
                schemeName,
                typeOfShare = SponsoringEmployer,
                holdShares = Acquisition,
                whenDidSchemeAcquire = Some(localDate),
                companyNameRelatedShares = companyName,
                companySharesCrn = ConditionalYesNo.no[String, Crn](noCrnReason),
                companyName,
                howManyShares = totalShares,
                identityType = Some(sellerType),
                recipientName = Some(sellerName),
                recipientDetails = Some(sellerDetails),
                recipientReasonNoDetails = None,
                sharesFromConnectedParty = None,
                costOfShares = money,
                shareIndependentValue = true,
                totalAssetValue = Some(money),
                sharesTotalIncome = money,
                mode = mode,
                viewOnlyUpdated = true
              )
            )
          }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
            .withName(s"render correct Acquisition ${mode.toString} view - $sellerType Seller")
        )
      }

      act.like(
        renderView(onPageLoad(mode), filledUserAnswersTransfer) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            SharesCheckAnswersUtils.viewModel(
              srn,
              index,
              schemeName,
              typeOfShare = Unquoted,
              holdShares = Transfer,
              whenDidSchemeAcquire = None,
              companyNameRelatedShares = companyName,
              companySharesCrn = ConditionalYesNo.yes[String, Crn](crn),
              companyName,
              howManyShares = totalShares,
              identityType = Some(IdentityType.Other),
              recipientName = Some(otherRecipientName),
              recipientDetails = Some(otherRecipientDescription),
              recipientReasonNoDetails = None,
              sharesFromConnectedParty = Some(true),
              costOfShares = money,
              shareIndependentValue = false,
              totalAssetValue = None,
              sharesTotalIncome = money,
              mode = mode,
              viewOnlyUpdated = true
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct Transfer ${mode.toString} view")
      )
      act.like(
        redirectNextPage(onSubmit(mode))
          .before {
            when(mockSaveService.save(any())(using any(), any())).thenReturn(Future.successful(()))
            MockPsrSubmissionService.submitPsrDetailsWithUA()
          }
          .after {
            verify(mockPsrSubmissionService, times(1))
              .submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
            verify(mockSaveService, times(1)).save(any())(using any(), any())
            reset(mockPsrSubmissionService)
            reset(mockSaveService)
          }
          .withName(s"redirect to next page when in $mode mode")
      )
      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )
      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
      redirectToPage(
        call = onPageLoad(mode),
        page = routes.CostOfSharesController.onPageLoad(srn, index, mode),
        userAnswers = incompleteUserAnswers,
        previousUserAnswers = emptyUserAnswers
      ).withName(s"redirect to list page when in $mode mode and incomplete data")
    }
  }

  "SharesCYAController in view only mode" - {

    val currentUserAnswers = filledUserAnswersContribution
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            SharesCheckAnswersUtils.viewModel(
              srn,
              index,
              schemeName,
              typeOfShare = ConnectedParty,
              holdShares = Contribution,
              whenDidSchemeAcquire = Some(localDate),
              companyNameRelatedShares = companyName,
              companySharesCrn = ConditionalYesNo.yes[String, Crn](crn),
              companyName,
              howManyShares = totalShares,
              identityType = Some(IdentityType.UKPartnership),
              recipientName = Some(companyName),
              recipientDetails = None,
              recipientReasonNoDetails = None,
              sharesFromConnectedParty = None,
              costOfShares = money,
              shareIndependentValue = true,
              totalAssetValue = None,
              sharesTotalIncome = money,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne)
            )
          )
      }
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.shares.routes.SharesListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only SharesListController page")
    )
  }
}
