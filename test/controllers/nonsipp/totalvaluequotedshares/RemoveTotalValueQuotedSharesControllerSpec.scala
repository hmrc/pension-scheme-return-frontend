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

package controllers.nonsipp.totalvaluequotedshares

import services.PsrSubmissionService
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import forms.YesNoPageFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.totalvaluequotedshares.{QuotedSharesManagedFundsHeldPage, TotalValueQuotedSharesPage}
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class RemoveTotalValueQuotedSharesControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.RemoveTotalValueQuotedSharesController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.RemoveTotalValueQuotedSharesController.onSubmit(srn, NormalMode)
  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(QuotedSharesManagedFundsHeldPage(srn), true)
    .unsafeSet(TotalValueQuotedSharesPage(srn), money)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveTotalValueQuotedSharesController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          RemoveTotalValueQuotedSharesController.form(injected[YesNoPageFormProvider]),
          RemoveTotalValueQuotedSharesController.viewModel(srn, NormalMode, money.displayAs)
        )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, filledUserAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
