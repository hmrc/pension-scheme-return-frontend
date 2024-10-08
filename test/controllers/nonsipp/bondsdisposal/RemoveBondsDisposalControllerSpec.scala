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

package controllers.nonsipp.bondsdisposal

import services.PsrSubmissionService
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import controllers.nonsipp.bondsdisposal.RemoveBondsDisposalController._
import forms.YesNoPageFormProvider
import models.HowDisposed
import pages.nonsipp.bondsdisposal.HowWereBondsDisposedOfPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.NameOfBondsPage

class RemoveBondsDisposalControllerSpec extends ControllerBaseSpec {

  private val bondIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.RemoveBondsDisposalController.onPageLoad(srn, bondIndex, disposalIndex)
  private lazy val onSubmit =
    routes.RemoveBondsDisposalController.onSubmit(srn, bondIndex, disposalIndex)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, bondIndex), nameOfBonds)
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex), HowDisposed.Transferred)

  private val userAnswersNameOfBond = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, bondIndex), nameOfBonds)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  "RemoveBondsDisposalController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, bondIndex, disposalIndex, nameOfBonds)
      )
    })

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(
      redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn), userAnswersNameOfBond)
        .withName("nameOfBonds")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
