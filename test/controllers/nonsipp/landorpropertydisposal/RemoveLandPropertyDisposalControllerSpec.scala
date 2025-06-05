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

package controllers.nonsipp.landorpropertydisposal

import services.PsrSubmissionService
import controllers.nonsipp.landorpropertydisposal.RemoveLandPropertyDisposalController._
import play.api.inject.bind
import views.html.YesNoPageView
import pages.nonsipp.landorpropertydisposal.{
  HowWasPropertyDisposedOfPage,
  LandOrPropertyDisposalProgress,
  LandPropertyDisposalCompletedPage
}
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{HowDisposed, NormalMode}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{when, _}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec
import utils.IntUtils.toInt
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage

import scala.concurrent.Future

class RemoveLandPropertyDisposalControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)
  private val testAddress = addressGen.sample.value
  private val methodOfDisposal = HowDisposed.Other(otherDetails)

  private lazy val onPageLoad =
    routes.RemoveLandPropertyDisposalController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.RemoveLandPropertyDisposalController.onSubmit(srn, index, disposalIndex, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), testAddress)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, index, disposalIndex), methodOfDisposal)
    .unsafeSet(LandPropertyDisposalCompletedPage(srn, index, disposalIndex), SectionCompleted)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, index, disposalIndex), SectionJourneyStatus.Completed)

  private val userAnswersAddress = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), testAddress)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  "RemoveLandPropertyDisposalController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, index, disposalIndex, testAddress.addressLine1, methodOfDisposal, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(
      redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn), userAnswersAddress)
        .withName("landOrPropertyChosenAddress")
    )

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .before {
          when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(())))
        }
        .after {
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
        }
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
