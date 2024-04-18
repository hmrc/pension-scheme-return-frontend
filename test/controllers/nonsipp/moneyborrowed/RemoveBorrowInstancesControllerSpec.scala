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

package controllers.nonsipp.moneyborrowed

import services.PsrSubmissionService
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.moneyborrowed._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class RemoveBorrowInstancesControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.RemoveBorrowInstancesController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveBorrowInstancesController.onSubmit(srn, index, NormalMode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, refineMV(1)), "lenderName")
    .unsafeSet(IsLenderConnectedPartyPage(srn, refineMV(1)), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, refineMV(1)), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, refineMV(1)), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, refineMV(1)), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), "reason")

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "RemoveBorrowInstancesController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          RemoveBorrowInstancesController.form(injected[YesNoPageFormProvider]),
          RemoveBorrowInstancesController.viewModel(srn, index, NormalMode, money.displayAs, "lenderName")
        )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )
    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
    )

    act.like(invalidForm(onSubmit, filledUserAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
