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

package controllers.nonsipp.loansmadeoroutstanding

import services.PsrSubmissionService
import play.api.inject.bind
import views.html.YesNoPageView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import models._
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec
import forms.YesNoPageFormProvider
import controllers.nonsipp.loansmadeoroutstanding.RemoveLoanController._

class RemoveLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.RemoveLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveLoanController.onSubmit(srn, index, NormalMode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, refineMV(1)), "recipientName1")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), amountOfTheLoan)
    .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, refineMV(2)), "recipientName2")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(2)), amountOfTheLoan)
    .unsafeSet(IdentityTypePage(srn, refineMV(3), IdentitySubject.LoanRecipient), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, refineMV(3)), "recipientName3")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(3)), amountOfTheLoan)
    .unsafeSet(LoansProgress(srn, refineMV(1)), SectionJourneyStatus.Completed)

  val prePopUserAnswersChecked: UserAnswers = filledUserAnswers.unsafeSet(LoanPrePopulated(srn, refineMV(1)), true)
  val prePopUserAnswersNotChecked: UserAnswers = filledUserAnswers.unsafeSet(LoanPrePopulated(srn, refineMV(1)), false)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "RemoveLoanController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, NormalMode, money.displayAs, "recipientName1")
        )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
          defaultUserAnswers.get(LoansProgress(srn, index)) mustBe None
        })
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
          filledUserAnswers.get(LoansProgress(srn, index)) mustBe Some(SectionJourneyStatus.Completed)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(
      saveAndContinue(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
          defaultUserAnswers.get(LoansProgress(srn, index)) mustBe None
        })
    )

    act.like(invalidForm(onSubmit, filledUserAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersChecked)
        .updateName(_ + " - Block removing checked Prepop loans")
    )

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersNotChecked)
        .updateName(_ + " - Block removing unchecked Prepop loans")
    )

  }
}
