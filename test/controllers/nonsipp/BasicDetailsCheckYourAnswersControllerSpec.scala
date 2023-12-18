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

package controllers.nonsipp

import cats.data.NonEmptyList
import cats.implicits.toShow
import config.Refined.Max3
import controllers.{nonsipp, BaseFixture, ControllerBaseSpec}
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import eu.timepit.refined.refineMV
import models.SchemeId.Srn
import models.{CheckMode, DateRange, Mode, NormalMode, SchemeDetails, SchemeMemberNumbers}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.WhichTaxYearPage
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.{PsrSubmissionService, SchemeDateService}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}
import views.html.CheckYourAnswersView

class BasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  class Fixture extends BaseFixture {
    val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
    val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

    override val additionalBindings: List[GuiceableModule] = List(
      bind[SchemeDateService].toInstance(mockSchemeDateService),
      bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
    )
  }

  private lazy val onPageLoad = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)
  private lazy val onSubmitInCheckMode = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, CheckMode)

  "BasicDetailsCheckYourAnswersController" - {

    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
      .unsafeSet(ActiveBankAccountPage(srn), true)

    val pensionSchemeId = pensionSchemeIdGen.sample.value
    val userAnswersWithManyMembers = userAnswersWithTaxYear
      .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(50, 60, 70))

    act.like(renderView(onPageLoad, userAnswersWithTaxYear, new Fixture) { implicit app => implicit request =>
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
          psaId.value
        )
      )
    }.before(f => MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))(f.mockSchemeDateService)))

    act.like(
      redirectNextPage(onSubmit, userAnswersWithTaxYear, new Fixture)
        .before { f =>
          MockSchemeDateService.returnPeriods(Some(NonEmptyList.of(dateRange)))(f.mockSchemeDateService)
          MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))(f.mockSchemeDateService)
          MockPSRSubmissionService.submitPsrDetails()(f.mockPsrSubmissionService)
        }
        .after(f => verify(f.mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any()))
    )

    act.like(
      redirectToPage(
        onSubmitInCheckMode,
        nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn),
        userAnswersWithManyMembers,
        new Fixture
      ).before { f =>
        MockSchemeDateService.returnPeriods(Some(NonEmptyList.of(dateRange)))(f.mockSchemeDateService)
        MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))(f.mockSchemeDateService)
        MockPSRSubmissionService.submitPsrDetails()(f.mockPsrSubmissionService)
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
          val vm = buildViewModel(
            activeBankAccount = true
          )

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
          activeBankAccount = true,
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
    pensionSchemeId: String = pensionSchemeIdGen.sample.value.value
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
    pensionSchemeId
  )
}
