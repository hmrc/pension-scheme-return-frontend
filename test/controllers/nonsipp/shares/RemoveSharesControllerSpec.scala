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

import services.PsrSubmissionService
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharePrePopulated}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import controllers.nonsipp.shares.RemoveSharesController._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class RemoveSharesControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private lazy val onPageLoad = routes.RemoveSharesController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveSharesController.onSubmit(srn, index, NormalMode)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)

  val prePopUserAnswersChecked: UserAnswers = userAnswers.unsafeSet(SharePrePopulated(srn, 1), true)
  val prePopUserAnswersNotChecked: UserAnswers = userAnswers.unsafeSet(SharePrePopulated(srn, 1), false)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveSharesController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, index, NormalMode, companyName)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersChecked)
        .updateName(_ + " - Block removing checked Prepop shares")
    )

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersNotChecked)
        .updateName(_ + " - Block removing unchecked Prepop shares")
    )

  }
}
