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

package controllers.nonsipp.receivetransfer

import services.PsrSubmissionService
import controllers.nonsipp.receivetransfer.ReportAnotherTransferInController.{form, viewModel}
import play.api.inject.bind
import views.html.YesNoPageView
import pages.nonsipp.receivetransfer.ReportAnotherTransferInPage
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when}
import config.RefinedTypes.{Max300, Max5}
import controllers.ControllerBaseSpec

import scala.concurrent.Future

class ReportAnotherTransferInControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    routes.ReportAnotherTransferInController.onPageLoad(srn, index.value, secondaryIndex.value, NormalMode)
  private lazy val onSubmit =
    routes.ReportAnotherTransferInController.onSubmit(srn, index.value, secondaryIndex.value, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "ReportAnotherTransferInController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            index,
            secondaryIndex,
            NormalMode,
            memberDetails.fullName
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, ReportAnotherTransferInPage(srn, index, secondaryIndex), true, userAnswers) {
        implicit app => implicit request =>
          injected[YesNoPageView].apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(
              srn,
              index,
              secondaryIndex,
              NormalMode,
              memberDetails.fullName
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
