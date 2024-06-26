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
import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage}
import config.Refined.Max3
import controllers.{nonsipp, ControllerBaseSpec}
import play.api.inject.bind
import cats.implicits.toShow
import eu.timepit.refined.refineMV
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage, WhichTaxYearPage}
import org.mockito.stubbing.OngoingStubbing
import models._
import play.api.i18n.Messages
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers.stubMessagesApi
import org.mockito.Mockito._
import cats.data.NonEmptyList
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}

class BasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)
  private lazy val onSubmitInCheckMode = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, CheckMode)
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

  private val updatedValues = SchemeMemberNumbers(4, 5, 6)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  "BasicDetailsCheckYourAnswersController" - {

    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
      .unsafeSet(ActiveBankAccountPage(srn), true)
    val pensionSchemeId = pensionSchemeIdGen.sample.value
    val userAnswersWithManyMembers = userAnswersWithTaxYear
      .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(50, 60, 70))

    act.like(renderView(onPageLoad, userAnswersWithTaxYear) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          NormalMode,
          schemeMemberNumbers,
          activeBankAccount = true,
          whyNoBankAccount = None,
          whichTaxYearPage = Some(dateRange),
          Left(dateRange),
          individualDetails.fullName,
          defaultSchemeDetails,
          psaId.value,
          psaId.isPSP,
          viewOnlyUpdated = false
        )
      )
    }.before(mockTaxYear(dateRange)))

    act.like(
      redirectNextPage(onSubmit, userAnswersWithTaxYear)
        .before {
          MockSchemeDateService.returnPeriods(Some(NonEmptyList.of(dateRange)))
          MockPSRSubmissionService.submitPsrDetails()
        }
        .after(
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
    )

    act.like(
      redirectToPage(
        onSubmitInCheckMode,
        nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn),
        userAnswersWithManyMembers
      ).before {
        MockSchemeDateService.returnPeriods(Some(NonEmptyList.of(dateRange)))
        MockPSRSubmissionService.submitPsrDetails()
      }
    )
    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "viewmodel" - {

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
      .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
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
              schemeMemberNumbers,
              activeBankAccount = true,
              whyNoBankAccount = None,
              whichTaxYearPage = Some(dateRange),
              Left(dateRange),
              individualDetails.fullName,
              defaultSchemeDetails,
              psaId.value,
              psaId.isPSP,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(HowManyMembersPage(srn, psaId), updatedValues)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              ViewOnlyMode,
              updatedValues,
              activeBankAccount = true,
              whyNoBankAccount = None,
              whichTaxYearPage = Some(dateRange),
              Left(dateRange),
              individualDetails.fullName,
              defaultSchemeDetails,
              psaId.value,
              psaId.isPSP,
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

    act.like(
      redirectToPage(
        onPreviousViewOnly,
        routes.BasicDetailsCheckYourAnswersController
          .onPageLoadViewOnly(srn, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to BasicDetailsCheckYourAnswersController for the previous submission"
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
    pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value
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
    pensionSchemeId.value,
    pensionSchemeId.isPSP,
    viewOnlyUpdated = false
  )

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))
}
