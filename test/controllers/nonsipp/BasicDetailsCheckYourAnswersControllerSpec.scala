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

import play.api.test.FakeRequest
import services._
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage}
import config.Refined.Max3
import controllers.{ControllerBaseSpec, TestUserAnswers}
import play.api.inject.bind
import cats.implicits.toShow
import eu.timepit.refined.refineMV
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import pages.nonsipp._
import models.backend.responses.PsrVersionsResponse
import org.mockito.stubbing.OngoingStubbing
import models._
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import play.api.i18n.Messages
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.stubMessagesApi
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import cats.data.NonEmptyList
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.Future

class BasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec with CommonTestValues with TestUserAnswers {

  private lazy val onPageLoad = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onPageLoadWithCheckMode = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode)
  private lazy val onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)
  private lazy val onPageLoadViewOnly = routes.BasicDetailsCheckYourAnswersController.onPageLoadViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onSubmitViewOnly = routes.BasicDetailsCheckYourAnswersController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.BasicDetailsCheckYourAnswersController.onPreviousViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  // This handles the latest return from the previous tax year
  override def beforeAll(): Unit =
    when(
      mockPsrRetrievalService.getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
    ).thenReturn(Future.successful(fullUserAnswers)) // Redirect Test 3 - full return submitted last tax year
      .thenReturn(Future.successful(skippedUserAnswers)) // Redirect Test 5 - skipped return submitted last tax year
  // Not triggered in any other tests

  override def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockSchemeDateService)
    reset(mockPsrVersionsService)
  }

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  "BasicDetailsCheckYourAnswersController" - {

    act.like(renderView(onPageLoad, currentTaxYearUserAnswersWithFewMembers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          NormalMode,
          memberNumbersUnderThreshold,
          activeBankAccount = true,
          whyNoBankAccount = None,
          whichTaxYearPage = Some(currentReturnTaxYear),
          Left(currentReturnTaxYear),
          individualDetails.fullName,
          defaultSchemeDetails,
          psaId,
          psaId.isPSP,
          viewOnlyUpdated = false,
          journeyByPassed = false
        )
      )
    }.before { mockTaxYear(currentReturnTaxYear) }
      .after {
        verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
        verify(mockSchemeDateService, never).submissionDateAsString(any())
        verify(mockSchemeDateService, never).returnPeriodsAsJsonString(any())(any())
        verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
      }
      .withName("when Initial UserAnswers empty"))

    val currentTaxYearWithFewMembersAlreadySubmittedUserAnswer = currentTaxYearUserAnswersWithFewMembers
      .unsafeSet(FbStatus(srn), Submitted)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(
        onPageLoadWithCheckMode,
        currentTaxYearWithFewMembersAlreadySubmittedUserAnswer,
        currentTaxYearWithFewMembersAlreadySubmittedUserAnswer
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            srn,
            CheckMode,
            memberNumbersUnderThreshold,
            activeBankAccount = true,
            whyNoBankAccount = None,
            whichTaxYearPage = Some(currentReturnTaxYear),
            Left(currentReturnTaxYear),
            individualDetails.fullName,
            defaultSchemeDetails,
            psaId,
            psaId.isPSP,
            viewOnlyUpdated = false,
            journeyByPassed = false
          )
        )
      }.before {
          mockTaxYear(currentReturnTaxYear)
        }
        .after {
          verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
          verify(mockSchemeDateService, never).submissionDateAsString(any())
          verify(mockSchemeDateService, never).returnPeriodsAsJsonString(any())(any())
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
        }
        .withName("when Initial UserAnswers & Current UserAnswers are same but bypassed journey requirements not meet")
    )

    val currentTaxYearWithManyMembersAlreadySubmittedUserAnswer = currentTaxYearUserAnswersWithManyMembers
      .unsafeSet(FbStatus(srn), Submitted)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(
        onPageLoadWithCheckMode,
        currentTaxYearWithManyMembersAlreadySubmittedUserAnswer,
        currentTaxYearWithManyMembersAlreadySubmittedUserAnswer
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            srn,
            CheckMode,
            memberNumbersOverThreshold,
            activeBankAccount = true,
            whyNoBankAccount = None,
            whichTaxYearPage = Some(currentReturnTaxYear),
            Left(currentReturnTaxYear),
            individualDetails.fullName,
            defaultSchemeDetails,
            psaId,
            psaId.isPSP,
            viewOnlyUpdated = false,
            journeyByPassed = true
          )
        )
      }.before {
          mockTaxYear(currentReturnTaxYear)
          mockSubmissionDate
          mockReturnPeriods
          mockVersions(Seq.empty[PsrVersionsResponse])
        }
        .after {
          verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
          verify(mockSchemeDateService, times(1)).submissionDateAsString(any())
          verify(mockSchemeDateService, times(1)).returnPeriodsAsJsonString(any())(any())
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
        }
        .withName("when Initial UserAnswers & Current UserAnswers are same and bypassed journey requirements meet")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "Redirect Tests - Task List / Declaration" - {

      "(1) should proceed to Task List when member threshold isn't reached" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.routes.TaskListController.onPageLoad(srn),
            currentTaxYearUserAnswersWithFewMembers,
            emptyUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(2) should proceed to Task List when 'full' return was submitted previously this tax year" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.routes.TaskListController.onPageLoad(srn),
            currentTaxYearUserAnswersWithManyMembers,
            fullUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(3) should proceed to Task List when 'full' return was submitted previously last tax year" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.routes.TaskListController.onPageLoad(srn),
            currentTaxYearUserAnswersWithManyMembers,
            emptyUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              mockVersions(versionsResponse)
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(4) should skip to Declaration when no returns of any kind were submitted this tax year or last tax year" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn),
            currentTaxYearUserAnswersWithManyMembers,
            emptyUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              mockVersions(Seq.empty[PsrVersionsResponse])
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(5) should skip to Declaration when 'skipped' return was submitted previously last tax year and this tax year" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn),
            currentTaxYearUserAnswersWithManyMembers,
            skippedUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              mockVersions(versionsResponse)
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(6) should redirect to JourneyRecovery if we couldn't get the current return's tax year" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.routes.JourneyRecoveryController.onPageLoad(),
            noTaxYearUserAnswers,
            emptyUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }

      "(7) should proceed to declaration when member numbers updated from below threshold to over threshold (e.g. loans exist)" - {

        act.like(
          redirectToPage(
            onSubmit,
            controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn),
            currentTaxYearUserAnswersWithManyMembers
              .unsafeSet(LoansMadeOrOutstandingPage(srn), false),
            emptyUserAnswers
          ).before {
              mockTaxYear(currentReturnTaxYear)
              mockVersions(Seq.empty[PsrVersionsResponse])
              MockPsrSubmissionService.submitPsrDetails()
            }
            .after {
              verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
              verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
              verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any(), any())(any(), any(), any())
            }
        )
      }
    }

    "viewModel" - {

      implicit val stubMessages: Messages = stubMessagesApi().preferred(FakeRequest())

      "should display the correct tax year" in {

        val vm = buildViewModel(
          taxYearOrAccountingPeriods = Left(dateRange)
        )

        vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
          "basicDetailsCheckYourAnswersController.schemeDetails.taxYear"
        )
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(dateRange.show)
      }

      "should display the correct accounting periods" in {

        val dateRange1 = dateRangeGen.sample.value
        val dateRange2 = dateRangeGen.sample.value
        val dateRange3 = dateRangeGen.sample.value

        val vm = buildViewModel(
          taxYearOrAccountingPeriods = Right(
            NonEmptyList.of(
              dateRange1 -> refineMV(1),
              dateRange2 -> refineMV(2),
              dateRange3 -> refineMV(3)
            )
          )
        )

        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(dateRange1.show)
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(dateRange2.show)
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(dateRange3.show)
      }

      "should display the correct active bank account value" - {
        "when active bank account is true" in {
          val vm = buildViewModel()

          vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
            "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount"
          )
          vm.page.sections.flatMap(_.rows.map(_.value match {
            case m: Message => m.key
          })) must contain("site.yes")
        }

        "when active bank account is false" in {
          val vm = buildViewModel(
            activeBankAccount = false
          )
          vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
            "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount"
          )
          vm.page.sections.flatMap(_.rows.map(_.value match {
            case m: Message => m.key
          })) must contain("site.no")
        }
      }

      "should display why no bank account correctly" in {
        val reason = "test reason"

        val vm = buildViewModel(
          whyNoBankAccount = Some(reason)
        )

        vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
          "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount"
        )
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(reason)
      }

      "should display the correct members numbers" in {
        val vm = buildViewModel(
          schemeMemberNumbers = schemeMemberNumbers
        )

        vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
          "basicDetailsCheckYourAnswersController.memberDetails.activeMembers"
        )
        vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
          "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers"
        )
        vm.page.sections.flatMap(_.rows.map(_.key.key)) must contain(
          "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers"
        )
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(schemeMemberNumbers.noOfActiveMembers.toString)
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(schemeMemberNumbers.noOfDeferredMembers.toString)
        vm.page.sections.flatMap(_.rows.map(_.value match {
          case m: Message => m.key
        })) must contain(
          schemeMemberNumbers.noOfPensionerMembers.toString
        )
      }
    }
  }

  "BasicDetailsCheckYourAnswersController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(ActiveBankAccountPage(srn), true)
    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(ActiveBankAccountPage(srn), true)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              ViewOnlyMode,
              memberNumbersUnderThreshold,
              activeBankAccount = true,
              whyNoBankAccount = None,
              whichTaxYearPage = Some(dateRange),
              Left(dateRange),
              individualDetails.fullName,
              defaultSchemeDetails,
              psaId,
              psaId.isPSP,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              journeyByPassed = false
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              ViewOnlyMode,
              memberNumbersOverThreshold,
              activeBankAccount = true,
              whyNoBankAccount = None,
              whichTaxYearPage = Some(dateRange),
              Left(dateRange),
              individualDetails.fullName,
              defaultSchemeDetails,
              psaId,
              psaId.isPSP,
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              journeyByPassed = false
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val updatedUserAnswersWithSubmissionDetails = updatedUserAnswers
      .unsafeSet(FbStatus(srn), Submitted)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = updatedUserAnswersWithSubmissionDetails,
        pureUserAnswers = updatedUserAnswersWithSubmissionDetails,
        optPreviousAnswers = None
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            srn,
            ViewOnlyMode,
            memberNumbersOverThreshold,
            activeBankAccount = true,
            whyNoBankAccount = None,
            whichTaxYearPage = Some(dateRange),
            Left(dateRange),
            individualDetails.fullName,
            defaultSchemeDetails,
            psaId,
            psaId.isPSP,
            viewOnlyUpdated = false,
            optYear = Some(yearString),
            optCurrentVersion = Some(submissionNumberTwo),
            optPreviousVersion = Some(submissionNumberOne),
            compilationOrSubmissionDate = Some(submissionDateTwo),
            journeyByPassed = true
          )
        )
      }.before {
          mockTaxYear(dateRange)
          mockSubmissionDate
          mockReturnPeriods
          mockVersions(Seq.empty)
        }
        .after {
          verify(mockSchemeDateService, times(1)).taxYearOrAccountingPeriods(any())(any())
          verify(mockSchemeDateService, times(1)).submissionDateAsString(any())
          verify(mockSchemeDateService, times(1)).returnPeriodsAsJsonString(any())(any())
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
        }
        .withName("OnPageLoadViewOnly renders ok with byPassed journey button")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).withName("Submit redirects to view only tasklist")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ReturnSubmittedController.onPageLoad(srn),
        currentTaxYearUserAnswersWithManyMembers
      ).before(
          mockVersions(Seq.empty[PsrVersionsResponse])
        )
        .after(
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
        )
        .withName(
          "Should proceed to return submitted page when previously many members with no details, currently many members"
        )
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne),
        currentTaxYearUserAnswersWithFewMembers
      ).after(
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
        )
        .withName(
          "Should proceed to view only task list when previously many members with no details, currently few members"
        )
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne),
        currentTaxYearUserAnswersWithManyMembers,
        previousTaxYearUserAnswersWithManyMembersWithMemberDetails
      ).after(
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
        )
        .withName(
          "Should proceed to view only task list when previously many members with details, currently many members"
        )
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne),
        currentTaxYearUserAnswersWithFewMembers,
        previousTaxYearUserAnswersWithManyMembersWithMemberDetails
      ).after(
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
        )
        .withName(
          "Should proceed to view only task list when previously many members with details, currently few members"
        )
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne),
        currentTaxYearUserAnswersWithManyMembers,
        previousTaxYearUserAnswersWithFewMembersWithMemberDetails
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName(
          "Should proceed to view only task list when previously few members with details, currently many members"
        )
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne),
        currentTaxYearUserAnswersWithFewMembers,
        previousTaxYearUserAnswersWithFewMembersWithMemberDetails
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName(
          "Should proceed to view only task list when previously few members with details, currently few members"
        )
    )
  }

  private def buildViewModel(
    srn: Srn = srn,
    mode: Mode = NormalMode,
    schemeMemberNumbers: SchemeMemberNumbers = schemeMemberNumbersGen.sample.value,
    activeBankAccount: Boolean = true,
    whyNoBankAccount: Option[String] = None,
    whichTaxYearPage: Option[DateRange] = Some(dateRange),
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]] = Left(dateRange),
    schemeAdminName: String = individualDetails.fullName,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value,
    journeyByPassed: Boolean = false
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    srn,
    mode,
    schemeMemberNumbers,
    activeBankAccount,
    whyNoBankAccount,
    whichTaxYearPage,
    taxYearOrAccountingPeriods,
    schemeAdminName,
    schemeDetails,
    pensionSchemeId,
    pensionSchemeId.isPSP,
    viewOnlyUpdated = false,
    journeyByPassed = journeyByPassed
  )

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))

  private def mockSubmissionDate: OngoingStubbing[String] =
    when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")

  private def mockReturnPeriods: OngoingStubbing[String] =
    when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")

  private def mockVersions(
    seqOfPsrVersionsResponse: Seq[PsrVersionsResponse]
  ): OngoingStubbing[Future[Seq[PsrVersionsResponse]]] =
    when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(seqOfPsrVersionsResponse))
}
