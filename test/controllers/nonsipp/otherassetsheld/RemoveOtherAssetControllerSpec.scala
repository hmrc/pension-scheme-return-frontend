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

package controllers.nonsipp.otherassetsheld

import services.PsrSubmissionService
import pages.nonsipp.otherassetsheld.{OtherAssetsPrePopulated, WhatIsOtherAssetPage}
import controllers.nonsipp.otherassetsheld.RemoveOtherAssetController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class RemoveOtherAssetControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private lazy val onPageLoad = routes.RemoveOtherAssetController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveOtherAssetController.onSubmit(srn, index, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index), otherName)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  val prePopUserAnswersChecked: UserAnswers = userAnswers.unsafeSet(OtherAssetsPrePopulated(srn, 1), true)
  val prePopUserAnswersNotChecked: UserAnswers = userAnswers.unsafeSet(OtherAssetsPrePopulated(srn, 1), false)

  override protected def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  "RemoveOtherAssetController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, index, otherName, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after {
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        }
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersChecked)
        .updateName(_ + " - Block removing checked Pre-pop other assets")
    )

    act.like(
      redirectToPage(onPageLoad, controllers.routes.UnauthorisedController.onPageLoad(), prePopUserAnswersNotChecked)
        .updateName(_ + " - Block removing unchecked Pre-pop other assets")
    )

  }
}
