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

package controllers.nonsipp

import services._
import pages.nonsipp.schemedesignatory._
import models.ConditionalYesNo._
import pages.nonsipp.shares._
import config.Refined._
import models.SchemeHoldShare._
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, _}
import pages.nonsipp.moneyborrowed._
import models.SponsoringOrConnectedParty._
import org.mockito.ArgumentMatchers._
import pages.nonsipp.memberdetails._
import org.mockito.Mockito.when
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import models.ManualOrUpload._
import models.IdentityType.{Other, _}
import viewmodels.models.MemberState.Active
import controllers.ControllerBaseSpec
import views.html.TaskListView
import models.TypeOfShares._
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.sharesdisposal._
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import play.api.inject
import uk.gov.hmrc.domain.Nino
import models.HowSharesDisposed._
import viewmodels.models.TaskListStatus.{Completed, TaskListStatus, Updated}
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject._

import scala.concurrent.Future

import java.time.format.DateTimeFormatter

class ViewOnlyTaskListControllerSpec extends ControllerBaseSpec with CommonTestValues {

  // Set up services
  private val mockSaveService: SaveService = mock[SaveService]
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      inject.bind[SaveService].toInstance(mockSaveService),
      inject.bind[SchemeDateService].toInstance(mockSchemeDateService),
      inject.bind[PsrVersionsService].toInstance(mockPsrVersionsService),
      inject.bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
    )

  override def beforeEach(): Unit = {
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(dateRange))
    when(mockPsrVersionsService.getVersions(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))
    when(mockPsrRetrievalService.getStandardPsrDetails(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(currentUA))
      .thenReturn(Future.successful(previousUA))
  }

  //Set test values
  private val index1of300: Max300 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)
  private val index1of50: Max50 = refineMV(1)
  private val name: String = "name"
  private val reason: String = "reason"
  private val numAccountingPeriods: Max3 = refineMV(1)
  private val submissionNumberTwo = 2
  private val submissionNumberOne = 1
  private val yearString: String = dateRange.from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  // Build userAnswers
  private val currentUA: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), dateRange) // automatically set
    // Section 1 - Scheme Details
    // (S1) Basic Details
    .unsafeSet(CheckReturnDatesPage(srn), true)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
    // (S1) Financial Details
    .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
    .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
    .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money)
    // Section 2 - Member Details
    .unsafeSet(PensionSchemeMembersPage(srn), Manual)
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index1of300), nino)
    .unsafeSet(MemberStatus(srn, index1of300), Active)
    // Section 3 - Member Payments
    // (S3) Employer Contributions
    // (S3) Unallocated Employer Contributions
    // (S3) Member Contributions
    // (S3) Transfers In
    // (S3) Transfers Out
    // (S3) PCLS
    // (S3) Pension Payments
    // (S3) Surrendered Benefits
    // Section 4 - Loans Made & Money Borrowed
    // (S4) Loans Made
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index1of5000), name)
    .unsafeSet(IndividualRecipientNinoPage(srn, index1of5000), ConditionalYesNo.yes[String, Nino](nino))
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Money](money))
    // (S4) Money Borrowed
    .unsafeSet(MoneyBorrowedPage(srn), true)
    .unsafeSet(LenderNamePage(srn, index1of5000), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index1of5000), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index1of5000), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index1of5000), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index1of5000), reason)
    // Section 5 - Shares
    // (S5) Shares
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
    .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), SponsoringEmployer)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index1of5000), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index1of5000), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index1of5000), name)
    .unsafeSet(SharesCompanyCrnPage(srn, index1of5000), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, index1of5000), classOfShares)
    .unsafeSet(HowManySharesPage(srn, index1of5000), totalShares)
    .unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), UKPartnership)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index1of5000), companyName)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index1of5000, SharesSeller), ConditionalYesNo.yes[String, Utr](utr))
    .unsafeSet(CostOfSharesPage(srn, index1of5000), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(TotalAssetValuePage(srn, index1of5000), money)
    .unsafeSet(SharesTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(SharesListPage(srn), false)
    // (S5) Shares Disposals
    .unsafeSet(SharesDisposalPage(srn), true)
    .unsafeSet(HowWereSharesDisposedPage(srn, index1of5000, index1of50), Sold)
    .unsafeSet(WhenWereSharesSoldPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(HowManySharesSoldPage(srn, index1of5000, index1of50), totalShares)
    .unsafeSet(TotalConsiderationSharesSoldPage(srn, index1of5000, index1of50), money)
    .unsafeSet(WhoWereTheSharesSoldToPage(srn, index1of5000, index1of50), Other)
    .unsafeSet(OtherBuyerDetailsPage(srn, index1of5000, index1of50), otherRecipientDetails)
    .unsafeSet(IsBuyerConnectedPartyPage(srn, index1of5000, index1of50), true)
    .unsafeSet(IndependentValuationPage(srn, index1of5000, index1of50), true)
    .unsafeSet(HowManyDisposalSharesPage(srn, index1of5000, index1of50), totalShares)
  // Section 6 - Land or Property
  // Section 7 - Bonds
  // Section 8 - Other Assets
  // Section 9 - Declaration

  private val previousUA: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), dateRange) // automatically set
    // Section 1 - Scheme Details
    // (S1) Basic Details
    .unsafeSet(CheckReturnDatesPage(srn), false)
    .unsafeSet(AccountingPeriodPage(srn, numAccountingPeriods, NormalMode), dateRange)
    .unsafeSet(ActiveBankAccountPage(srn), false)
    .unsafeSet(WhyNoBankAccountPage(srn), reason)
    .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
    // (S1) Financial Details
    .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(money, Money(2)))
    .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(money, Money(3)))
    .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), Money(1))
    // Section 2 - Member Details
    .unsafeSet(PensionSchemeMembersPage(srn), Upload)
    .unsafeSet(CheckMemberDetailsFilePage(srn), true)
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
    .unsafeSet(NoNINOPage(srn, index1of300), reason)
    .unsafeSet(MemberStatus(srn, index1of300), Active)
    // Section 3 - Member Payments
    // (S3) Employer Contributions
    // (S3) Unallocated Employer Contributions
    // (S3) Member Contributions
    // (S3) Transfers In
    // (S3) Transfers Out
    // (S3) PCLS
    // (S3) Pension Payments
    // (S3) Surrendered Benefits
    // Section 4 - Loans Made & Money Borrowed
    // (S4) Loans Made
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LoanRecipient), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index1of5000), Sponsoring)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Money](money))
    // (S4) Money Borrowed
    .unsafeSet(MoneyBorrowedPage(srn), true)
    .unsafeSet(LenderNamePage(srn, index1of5000), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index1of5000), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index1of5000), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index1of5000), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index1of5000), reason)
    // Section 5 - Shares
    // (S5) Shares
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
    .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), Unquoted)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index1of5000), Contribution)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index1of5000), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index1of5000), name)
    .unsafeSet(SharesCompanyCrnPage(srn, index1of5000), ConditionalYesNo.no[String, Crn](reason))
    .unsafeSet(ClassOfSharesPage(srn, index1of5000), classOfShares)
    .unsafeSet(HowManySharesPage(srn, index1of5000), totalShares)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(CostOfSharesPage(srn, index1of5000), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(SharesTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(SharesListPage(srn), false)
    // (S5) Shares Disposals
    .unsafeSet(SharesDisposalPage(srn), true)
    .unsafeSet(HowWereSharesDisposedPage(srn, index1of5000, index1of50), Redeemed)
    .unsafeSet(WhenWereSharesRedeemedPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(HowManySharesRedeemedPage(srn, index1of5000, index1of50), totalShares)
    .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, index1of5000, index1of50), money)
    .unsafeSet(HowManyDisposalSharesPage(srn, index1of5000, index1of50), totalShares)
  // Section 6 - Land or Property
  // Section 7 - Bonds
  // Section 8 - Other Assets
  // Section 9 - Declaration

  "ViewOnlyTaskListController" - {

    lazy val viewModelSubmissionTwo = ViewOnlyTaskListController.viewModel(
      srn,
      schemeName,
      dateRange,
      currentUA,
      previousUA,
      submissionNumberTwo
    )

    lazy val onPageLoadSubmissionTwo = routes.ViewOnlyTaskListController.onPageLoad(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

    act.like(renderView(onPageLoadSubmissionTwo, currentUA) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModelSubmissionTwo)
    }.withName("onPageLoad renders ok with indexes /2/1"))

    act.like(journeyRecoveryPage(onPageLoadSubmissionTwo).updateName("onPageLoad " + _))

    lazy val viewModelVersionOne = ViewOnlyTaskListController.viewModel(
      srn,
      schemeName,
      dateRange,
      currentUA,
      previousUA,
      submissionNumberOne
    )

    lazy val onPageLoadSubmissionOne = routes.ViewOnlyTaskListController.onPageLoad(
      srn,
      yearString,
      submissionNumberOne,
      0
    )

    act.like(renderView(onPageLoadSubmissionOne, currentUA) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModelVersionOne)
    }.withName("onPageLoad renders ok with indexes /1/0"))

    // TODO: implement lower-level journey navigation in future ticket, until then Unauthorised page used for all links

    "Status, content, and URL tests" - {

      "Section 1 - Scheme details" - {

        "(S1) Basic details" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              0,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.details.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              0,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.details.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S1) Financial details" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              0,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.finances.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              0,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.finances.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      "Section 2 - Members" - {

        "Completed" in {
          testViewModel(
            currentUA,
            currentUA,
            1,
            0,
            expectedStatus = Completed,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.view.details.title",
            expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
          )
        }

        "Updated" in {
          testViewModel(
            currentUA,
            previousUA,
            1,
            0,
            expectedStatus = Updated,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.view.details.title",
            expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
          )
        }
      }

      "Section 3 - Member payments" - {

        "(S3) Employer contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.employercontributions.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              0,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.employercontributions.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Unallocated employer contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.unallocatedcontributions.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              1,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.unallocatedcontributions.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Member contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              2,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.memberContributions.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              2,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.memberContributions.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Transfers in" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              3,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersreceived.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              3,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersreceived.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Transfers out" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              4,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersout.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              4,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersout.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Pension commencement lump sum" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              5,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.pcls.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              5,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.pcls.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Pension payments" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              6,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.payments.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              6,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.payments.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }

        "(S3) Surrendered benefits" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              7,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.surrenderedbenefits.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

//          "Updated" in {
//            testViewModel(
//              currentUA,
//              previousUA,
//              2,
//              7,
//              expectedStatus = Updated,
//              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
//              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.surrenderedbenefits.title",
//              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
//            )
//          }
        }
      }

      "Section 4 - Loans made and money borrowed" - {

        "(S4) Loans made" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              3,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.loansmade.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              3,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.loansmade.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S4) Money borrowed" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              3,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.moneyborrowed.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              3,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.moneyborrowed.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      "Section 5 - Shares" - {

        "(S5) Shares" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              4,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.shares.view.sponsoringemployer.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              4,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.shares.view.sponsoringemployer.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S5) Shares Disposals" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              4,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              4,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      // Land or property

      // Bonds

      // Other assets

      // Declaration

    }
  }

  private def testViewModel(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String
  ): Object = {
    val customViewModel = ViewOnlyTaskListController.viewModel(
      srn,
      schemeName,
      dateRange,
      currentUA,
      previousUA,
      submissionNumberTwo
    )
    val sections = customViewModel.page.sections.toList
    sections(sectionIndex).title.key mustBe expectedTitleKey
    sections(sectionIndex).items.fold(
      _ => "",
      list => {
        val item = list.toList(itemIndex)
        item.status mustBe expectedStatus
        item.link.content.key mustBe expectedLinkContentKey
        item.link.url mustBe expectedLinkUrl
      }
    )
  }
}
