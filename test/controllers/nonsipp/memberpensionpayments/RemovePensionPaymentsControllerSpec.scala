/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.nonsipp.memberpensionpayments

import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NameDOB
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.YesNoPageView

import scala.concurrent.Future

class RemovePensionPaymentsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemovePensionPaymentsController.onPageLoad(srn, refineMV(1))
  private lazy val onSubmit = routes.RemovePensionPaymentsController.onSubmit(srn, refineMV(1))

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(1)), money)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemovePensionPaymentsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        RemovePensionPaymentsController.form(injected[YesNoPageFormProvider]),
        RemovePensionPaymentsController.viewModel(srn, refineMV(1), money, memberDetails.fullName)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      continueNoSave(onSubmit, userAnswers, "value" -> "false")
        .after(verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any())(any(), any(), any()))
    )

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .after(verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any()))
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
