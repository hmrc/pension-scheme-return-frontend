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

package controllers.nonsipp.sharesdisposal

import services.PsrSubmissionService
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.HowWereSharesDisposedPage
import forms.YesNoPageFormProvider
import models.{HowSharesDisposed, NormalMode}
import controllers.nonsipp.sharesdisposal.RemoveShareDisposalController._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class RemoveShareDisposalControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val disposalIndex = 1
  private val methodOfDisposal = HowSharesDisposed.Redeemed

  private lazy val onPageLoad = routes.RemoveShareDisposalController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.RemoveShareDisposalController.onSubmit(srn, index, disposalIndex, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, index, disposalIndex), HowSharesDisposed.Redeemed)

  private val userAnswersRelatedShares = defaultUserAnswers
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, index, disposalIndex), methodOfDisposal)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveShareDisposalController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, index, disposalIndex, companyName, methodOfDisposal, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      ).withName("relatedShares")
    )

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .after {
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(using
            any(),
            any(),
            any()
          )
          reset(mockPsrSubmissionService)
        }
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
