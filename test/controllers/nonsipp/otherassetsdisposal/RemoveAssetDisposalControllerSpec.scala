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

package controllers.nonsipp.otherassetsdisposal

import services.PsrSubmissionService
import controllers.nonsipp.otherassetsdisposal.RemoveAssetDisposalController._
import pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class RemoveAssetDisposalControllerSpec extends ControllerBaseSpec {

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.RemoveAssetDisposalController.onPageLoad(srn, assetIndex, disposalIndex)
  private lazy val onSubmit =
    routes.RemoveAssetDisposalController.onSubmit(srn, assetIndex, disposalIndex)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers.unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), nameOfAsset)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  "RemoveAssetDisposalController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, assetIndex, disposalIndex, nameOfAsset)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
