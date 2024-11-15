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

import services.PsrVersionsService
import models.ConditionalYesNo._
import pages.nonsipp.shares.{DidSchemeHoldAnySharesPage, SharesCompleted}
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, OtherAssetsHeldPage, WhatIsOtherAssetPage}
import views.html.TaskListView
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import eu.timepit.refined.refineMV
import models.backend.responses.{PsrVersionsResponse, ReportStatus}
import models._
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.moneyborrowed._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus, TaskListStatus}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.otherassetsdisposal._
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.memberdetails._
import pages.nonsipp.totalvaluequotedshares.{QuotedSharesManagedFundsHeldPage, TotalValueQuotedSharesPage}
import org.mockito.Mockito.when
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage, UnregulatedOrConnectedBondsHeldPage}
import config.RefinedTypes.{Max3, Max5000}
import controllers.ControllerBaseSpec
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.sharesdisposal._
import pages.nonsipp.{CheckReturnDatesPage, FbVersionPage, WhichTaxYearPage}
import play.api.inject
import viewmodels.models.TaskListStatus.TaskListStatus
import pages.nonsipp.common.IdentityTypePage

import scala.concurrent.Future

import java.time.LocalDateTime

class TaskListControllerSpec extends ControllerBaseSpec with CommonTestValues {

  val pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value
  val index1of3: Max3 = refineMV(1)
  val index1of5000: Max5000 = refineMV(1)
  val reason = "reason"

  private val mockPsrVersionsService = mock[PsrVersionsService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      inject.bind[PsrVersionsService].toInstance(mockPsrVersionsService)
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Seq()))
  }

  "TaskListController" - {
    val populatedUserAnswers = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(CheckReturnDatesPage(srn), true)
      .unsafeSet(ActiveBankAccountPage(srn), true)
      .unsafeSet(HowManyMembersPage.bySrn(srn), schemeMemberNumbers)
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(FbVersionPage(srn), commonVersion)
    val populatedUserAnswersV2 = populatedUserAnswers.unsafeSet(FbVersionPage(srn), "002")
    val populatedUserAnswersV3 = populatedUserAnswers.unsafeSet(FbVersionPage(srn), "003")

    lazy val defaultViewModel = TaskListController.viewModel(
      srn,
      schemeName,
      dateRange.from,
      dateRange.to,
      populatedUserAnswers,
      pensionSchemeId,
      hasHistory = false,
      noChangesSincePreviousVersion = false,
      None,
      None
    )
    lazy val onPageLoad = routes.TaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(defaultViewModel, schemeName)
    }.withName("task list renders OK when no version response"))

    act.like(
      renderView(onPageLoad, populatedUserAnswersV3) { implicit app => implicit request =>
        val view = injected[TaskListView]
        view(defaultViewModel, schemeName)
      }.withName("task list renders OK when version response without any submitted")
        .before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any()))
            .thenReturn(
              Future.successful(
                Seq(
                  PsrVersionsResponse(
                    startDate = None,
                    reportFormBundleNumber = commonFbNumber.replace('1', '3'),
                    reportVersion = commonVersion.toInt + 2,
                    reportStatus = ReportStatus.ReportStatusCompiled,
                    compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
                    reportSubmitterDetails = None,
                    psaDetails = None
                  )
                )
              )
            )
        )
    )

    act.like(
      renderView(
        onPageLoad,
        populatedUserAnswersV2,
        optPreviousAnswers = None
      ) { implicit app => implicit request =>
        val view = injected[TaskListView]
        view(
          TaskListController.viewModel(
            srn,
            schemeName,
            dateRange.from,
            dateRange.to,
            populatedUserAnswers,
            pensionSchemeId,
            hasHistory = true,
            noChangesSincePreviousVersion = true,
            None,
            None
          ),
          schemeName
        )
      }.withName("task list renders OK when version response with any submitted")
        .before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any()))
            .thenReturn(
              Future.successful(versionsResponse)
            )
        )
    )

    act.like(
      renderView(onPageLoad, populatedUserAnswersV3, optPreviousAnswers = Some(populatedUserAnswers)) {
        implicit app => implicit request =>
          val view = injected[TaskListView]
          view(
            TaskListController.viewModel(
              srn,
              schemeName,
              dateRange.from,
              dateRange.to,
              populatedUserAnswers,
              pensionSchemeId,
              hasHistory = true,
              noChangesSincePreviousVersion = true,
              None,
              None
            ),
            schemeName
          )
      }.withName("task list renders OK when version response with any submitted but max version compiled")
        .before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any()))
            .thenReturn(
              Future.successful(versionsResponseInProgress)
            )
        )
    )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.JourneyRecoveryController
          .onPageLoad(Some(RedirectUrl(controllers.routes.OverviewController.onPageLoad(srn).url))),
        defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)
      )
    )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.OverviewController.onPageLoad(srn),
        populatedUserAnswers,
        populatedUserAnswers
      ).before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any(), any()))
            .thenReturn(
              Future.successful(versionsResponseInProgress)
            )
        )
        .withName("task list redirects to overview page when a historical submission is in user answers")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "schemeDetailsSection - basic details" - {

      "inProgress" - {

        "stopped at check dates page" in {
          testViewModel(
            defaultUserAnswers,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped after dates page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at active bank account page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode).url
          )

        }

        "stopped at reason no bank account page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(ActiveBankAccountPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at reason no bank account page (answer changed) when how many members already populated" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(ActiveBankAccountPage(srn), false)
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at check return dates page (answer changed) when how many members already populated" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)
              .unsafeSet(ActiveBankAccountPage(srn), true)
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at how many members page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)
              .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
              .unsafeSet(ActiveBankAccountPage(srn), false)
              .unsafeSet(WhyNoBankAccountPage(srn), reason)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "recorded" - {

        "with CheckReturnDatesPage true and ActiveBankAccountPage true" in {

          val userAnswersWithHowManyMembers =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              // AccountingPeriodPage not set
              .unsafeSet(ActiveBankAccountPage(srn), true)
              // WhyNoBankAccount not set
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersWithHowManyMembers,
            0,
            0,
            expectedStatus = TaskListStatus.Recorded,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          )
        }

        "with CheckReturnDatesPage true and ActiveBankAccountPage false" in {

          val userAnswersWithHowManyMembers =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
              // AccountingPeriodPage has been set here even though CheckReturnDatesPage is set to true because this
              // mimics the transformation that occurs when we GET a submitted return from ETMP.
              .unsafeSet(ActiveBankAccountPage(srn), false)
              .unsafeSet(WhyNoBankAccountPage(srn), reason)
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersWithHowManyMembers,
            0,
            0,
            expectedStatus = TaskListStatus.Recorded,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          )
        }

        "with CheckReturnDatesPage false and ActiveBankAccountPage true" in {

          val userAnswersWithHowManyMembers =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)
              .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
              .unsafeSet(ActiveBankAccountPage(srn), true)
              // WhyNoBankAccount not set
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersWithHowManyMembers,
            0,
            0,
            expectedStatus = TaskListStatus.Recorded,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          )
        }

        "with CheckReturnDatesPage false and ActiveBankAccountPage false" in {

          val userAnswersWithHowManyMembers =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)
              .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
              .unsafeSet(ActiveBankAccountPage(srn), false)
              .unsafeSet(WhyNoBankAccountPage(srn), reason)
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersWithHowManyMembers,
            0,
            0,
            expectedStatus = TaskListStatus.Recorded,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.details.title",
            expectedLinkUrl =
              controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          )
        }
      }
    }

    "schemeDetailsSection - financial details" - {

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          0,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
          expectedLinkContentKey = "nonsipp.tasklist.schemedetails.add.finances.title",
          expectedLinkUrl =
            controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
        )
      }

      "inProgress" - {

        "stopped after how much cash page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))

          testViewModel(
            userAnswersPopulated,
            0,
            1,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped after value of assets page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))
              .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))

          testViewModel(
            userAnswersPopulated,
            0,
            1,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
            expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
              .onPageLoad(srn, NormalMode)
              .url
          )
        }
      }

      "recorded" in {
        val userAnswersWithPopulatedAnswers =
          defaultUserAnswers
            .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))
            .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))
            .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), Money(1))

        testViewModel(
          userAnswersWithPopulatedAnswers,
          0,
          1,
          expectedStatus = TaskListStatus.Recorded,
          expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
          expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
          expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }
    }

    "membersSection" - {

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          1,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.members.title",
          expectedLinkContentKey = "nonsipp.tasklist.members.add.details.title",
          expectedLinkUrl = controllers.nonsipp.memberdetails.routes.PensionSchemeMembersController.onPageLoad(srn).url
        )
      }

      "inProgress" - {

        "DoesMemberHaveNinoPage is missing" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.DoesSchemeMemberHaveNINOController
              .onPageLoad(srn, refineMV(1), NormalMode)
              .url
          )
        }

        "MemberDetailsNinoPage is missing" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.MemberDetailsNinoController
              .onPageLoad(srn, refineMV(1), NormalMode)
              .url
          )
        }

        "NoNINOPage is missing" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.NoNINOController
              .onPageLoad(srn, refineMV(1), NormalMode)
              .url
          )
        }
      }

      "Recorded" - {

        "with NINO" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)
            .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)
            .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.Recorded(1, "members"),
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.SchemeMembersListController
              .onPageLoad(srn, 1, ManualOrUpload.Manual)
              .url
          )
        }

        "with reason for no NINO" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)
            .unsafeSet(NoNINOPage(srn, refineMV(1)), reason)
            .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.Recorded(1, "members"),
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.SchemeMembersListController
              .onPageLoad(srn, 1, ManualOrUpload.Manual)
              .url
          )
        }
      }
    }

    "loansSection" - { // Also tested in TaskListStatusUtilsSpec

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          3,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.add.loansmade.title",
          expectedLinkUrl = controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress" in {
        val userAnswersWithLoans =
          defaultUserAnswers
            .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
            .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.Individual)
            .unsafeSet(IndividualRecipientNamePage(srn, refineMV(1)), "First Last")

        testViewModel(
          userAnswersWithLoans,
          3,
          0,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.change.loansmade.title",
          expectedLinkUrl = controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, refineMV(1), NormalMode, IdentitySubject.LoanRecipient)
            .url
        )
      }

      "Recorded" in {
        val userAnswersWithLoans =
          defaultUserAnswers
            .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
            .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.Individual)
            .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))
            .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
            .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(2)), ConditionalYesNo.yes[Unit, Money](money))

        testViewModel(
          userAnswersWithLoans,
          3,
          0,
          expectedStatus = TaskListStatus.Recorded(2, "loans"),
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.change.loansmade.title",
          expectedLinkUrl =
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, 1, NormalMode).url
        )
      }
    }

    "borrowingSection" - { // Also tested in TaskListStatusUtilsSpec
      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          3,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.add.moneyborrowed.title",
          expectedLinkUrl = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress" in {
        val userAnswersWithBorrowings =
          defaultUserAnswers
            .unsafeSet(MoneyBorrowedPage(srn), true)
            .unsafeSet(LenderNamePages(srn), Map("0" -> lenderName))

        testViewModel(
          userAnswersWithBorrowings,
          3,
          1,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.change.moneyborrowed.title",
          expectedLinkUrl = controllers.nonsipp.moneyborrowed.routes.LenderNameController
            .onPageLoad(srn, refineMV(1), NormalMode)
            .url
        )
      }

      "recorded" in {
        val userAnswersWithBorrowings =
          defaultUserAnswers
            .unsafeSet(MoneyBorrowedPage(srn), true)
            .unsafeSet(LenderNamePage(srn, index1of5000), lenderName)
            .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index1of5000), reason)

        testViewModel(
          userAnswersWithBorrowings,
          3,
          1,
          expectedStatus = TaskListStatus.Recorded(1, "borrowings"),
          expectedTitleKey = "nonsipp.tasklist.loans.title",
          expectedLinkContentKey = "nonsipp.tasklist.loans.change.moneyborrowed.title",
          expectedLinkUrl =
            controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, 1, NormalMode).url
        )
      }
    }

    "shares section" - { // Also tested in TaskListStatusUtilsSpec
      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          4,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.shares.add.sponsoringemployer.title",
          expectedLinkUrl = controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "Recorded" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)

        testViewModel(
          userAnswersWithData,
          4,
          0,
          expectedStatus = TaskListStatus.Recorded(0, ""),
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.shares.change.sponsoringemployer.title",
          expectedLinkUrl = controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }
    }

    "sharesDisposalSection" - {

      "notStarted and only visible with a share existing" in {
        testViewModel(
          defaultUserAnswers.unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted),
          4,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.add.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "Recorded (with 'No' selection on first page) and only visible with a share existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(SharesDisposalPage(srn), false)
          .unsafeSet(SharesDisposalCompleted(srn), SectionCompleted)

        testViewModel(
          userAnswersWithData,
          4,
          1,
          expectedStatus = TaskListStatus.Recorded(0, ""),
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress (with only 1 incomplete disposal) and only visible with a share existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(SharesDisposalPage(srn), true)
          // Shares 1 - Disposal 1 - Incomplete journey:
          .unsafeSet(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1)), HowSharesDisposed.Sold)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))

        testViewModel(
          userAnswersWithData,
          4,
          1,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "Recorded (with 1 complete and 1 incomplete disposal) and only visible with a share existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(SharesDisposalPage(srn), true)
          // Shares 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1)), HowSharesDisposed.Transferred)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(HowManyDisposalSharesPage(srn, refineMV(1), refineMV(1)), 1)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
          // Shares 1 - Disposal 2 - Incomplete journey:
          .unsafeSet(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(2)), HowSharesDisposed.Sold)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(2)), SectionJourneyStatus.InProgress("x"))

        testViewModel(
          userAnswersWithData,
          4,
          1,
          expectedStatus = TaskListStatus.Recorded(1, "disposals"),
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoad(srn, page = 1)
            .url
        )
      }

      "Recorded (with only 1 complete disposal) and only visible with a share existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(SharesDisposalPage(srn), true)
          // Shares 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1)), HowSharesDisposed.Transferred)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(HowManyDisposalSharesPage(srn, refineMV(1), refineMV(1)), 1)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)

        testViewModel(
          userAnswersWithData,
          4,
          1,
          expectedStatus = TaskListStatus.Recorded(1, "disposals"),
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoad(srn, page = 1)
            .url
        )
      }

      "notStarted (with removal of only complete disposal) and only visible with a share existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(SharesDisposalPage(srn), true)
          // Shares 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1)), HowSharesDisposed.Transferred)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(HowManyDisposalSharesPage(srn, refineMV(1), refineMV(1)), 1)
          .unsafeSet(SharesDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
          // Shares 1 - Disposal 1 - Removal:
          .unsafeRemove(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1)))
          .unsafeRemove(SharesDisposalCompleted(srn))
          .unsafeRemove(SharesDisposalProgress(srn, refineMV(1), refineMV(1)))

        testViewModel(
          userAnswersWithData,
          4,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.shares.title",
          expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.add.title",
          expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }
    }

    "quotedSharesSection" - { // Also tested in TaskListStatusUtilsSpec

      "notStarted and only visible with shares answered" in {
        testViewModel(
          defaultUserAnswers.unsafeSet(DidSchemeHoldAnySharesPage(srn), true),
          7,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassets.add.quotedshares.title",
          expectedLinkUrl = controllers.nonsipp.totalvaluequotedshares.routes.QuotedSharesManagedFundsHeldController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "Recorded and only visible with shares answered" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(QuotedSharesManagedFundsHeldPage(srn), true)
            .unsafeSet(TotalValueQuotedSharesPage(srn), money)
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)

        testViewModel(
          userAnswersWithData,
          7,
          0,
          expectedStatus = TaskListStatus.Recorded,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassets.change.quotedshares.title",
          expectedLinkUrl = controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
            .onPageLoad(srn)
            .url
        )
      }
    }

    "bonds section" - { // Also tested in TaskListStatusUtilsSpec

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.bonds.title",
          expectedLinkContentKey = "nonsipp.tasklist.bonds.add.unregulatedorconnected.title",
          expectedLinkUrl = controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
            .unsafeSet(NameOfBondsPage(srn, index1of5000), nameOfBonds)

        testViewModel(
          userAnswersWithData,
          6,
          0,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.bonds.title",
          expectedLinkContentKey = "nonsipp.tasklist.bonds.change.unregulatedorconnected.title",
          expectedLinkUrl = controllers.nonsipp.bonds.routes.NameOfBondsController
            .onPageLoad(srn, index1of5000, NormalMode)
            .url
        )
      }

      "recorded" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
            .unsafeSet(NameOfBondsPage(srn, index1of5000), nameOfBonds)
            .unsafeSet(BondsCompleted(srn, index1of5000), SectionCompleted)

        testViewModel(
          userAnswersWithData,
          6,
          0,
          expectedStatus = TaskListStatus.Recorded(1, "bonds"),
          expectedTitleKey = "nonsipp.tasklist.bonds.title",
          expectedLinkContentKey = "nonsipp.tasklist.bonds.change.unregulatedorconnected.title",
          expectedLinkUrl = controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoad(srn, 1, NormalMode)
            .url
        )
      }
    }

    "other assets section" - { // Also tested in TaskListStatusUtilsSpec

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          7,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassets.add.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(OtherAssetsHeldPage(srn), true)
            .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), otherAssetDescription)

        testViewModel(
          userAnswersWithData,
          7,
          0,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassets.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
            .onPageLoad(srn, index1of5000, NormalMode)
            .url
        )
      }

      "recorded" in {
        val userAnswersWithData =
          defaultUserAnswers
            .unsafeSet(OtherAssetsHeldPage(srn), true)
            .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), otherAssetDescription)
            .unsafeSet(OtherAssetsCompleted(srn, index1of5000), SectionCompleted)

        testViewModel(
          userAnswersWithData,
          7,
          0,
          expectedStatus = TaskListStatus.Recorded(1, "otherAssets"),
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassets.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoad(srn, 1, NormalMode)
            .url
        )
      }
    }

    "otherAssetsDisposalSection" - {

      "notStarted and only visible with an asset existing" in {
        testViewModel(
          defaultUserAnswers.unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted),
          7,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.add.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "recorded (with 'No' selection on first page) and only visible with an asset existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(OtherAssetsDisposalPage(srn), false)
          .unsafeSet(OtherAssetsDisposalCompleted(srn), SectionCompleted)

        testViewModel(
          userAnswersWithData,
          7,
          1,
          expectedStatus = TaskListStatus.Recorded(0, ""),
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "inProgress (with only 1 incomplete disposal) and only visible with an asset existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(OtherAssetsDisposalPage(srn), true)
          // Other Assets 1 - Disposal 1 - Incomplete journey:
          .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Sold)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))

        testViewModel(
          userAnswersWithData,
          7,
          1,
          expectedStatus = TaskListStatus.InProgress,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }

      "recorded (with 1 complete and 1 incomplete disposal) and only visible with an asset existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(OtherAssetsDisposalPage(srn), true)
          // Other Asset 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Transferred)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(1), refineMV(1)), true)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
          // Other Assets 1 - Disposal 2 - Incomplete journey:
          .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(2)), HowDisposed.Sold)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(2)), SectionJourneyStatus.InProgress("x"))

        testViewModel(
          userAnswersWithData,
          7,
          1,
          expectedStatus = TaskListStatus.Recorded(1, "disposals"),
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
            .onPageLoad(srn, page = 1)
            .url
        )
      }

      "recorded (with only 1 complete disposal) and only visible with an asset existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(OtherAssetsDisposalPage(srn), true)
          // Other Asset 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Transferred)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(1), refineMV(1)), true)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)

        testViewModel(
          userAnswersWithData,
          7,
          1,
          expectedStatus = TaskListStatus.Recorded(1, "disposals"),
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.change.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
            .onPageLoad(srn, page = 1)
            .url
        )
      }

      "notStarted (with removal of only complete disposal) and only visible with an asset existing" in {
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          .unsafeSet(OtherAssetsDisposalPage(srn), true)
          // Other Asset 1 - Disposal 1 - Complete journey:
          .unsafeSet(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Transferred)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.InProgress("x"))
          .unsafeSet(AnyPartAssetStillHeldPage(srn, refineMV(1), refineMV(1)), true)
          .unsafeSet(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
          // Other Asset 1 - Disposal 1 - Removal:
          .unsafeRemove(HowWasAssetDisposedOfPage(srn, refineMV(1), refineMV(1)))
          .unsafeRemove(OtherAssetsDisposalCompleted(srn))
          .unsafeRemove(OtherAssetsDisposalProgress(srn, refineMV(1), refineMV(1)))

        testViewModel(
          userAnswersWithData,
          7,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.otherassets.title",
          expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.add.title",
          expectedLinkUrl = controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }
    }
  }

  private def testViewModel(
    userAnswersPopulated: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String
  ): Object = {
    val customViewModel = TaskListController.viewModel(
      srn,
      schemeName,
      dateRange.from,
      dateRange.to,
      userAnswersPopulated,
      pensionSchemeId,
      hasHistory = false,
      noChangesSincePreviousVersion = false,
      None,
      None
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
