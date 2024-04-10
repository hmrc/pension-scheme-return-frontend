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

package controllers.nonsipp.memberpensionpayments

import controllers.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsController._
import services.PsrSubmissionService
import config.Refined._
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.MoneyView
import forms.MoneyFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import eu.timepit.refined.refineMV

import scala.concurrent.Future

class TotalAmountPensionPaymentsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val userAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  private val mockPSRSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    routes.TotalAmountPensionPaymentsController.onPageLoad(srn, index, NormalMode)

  private lazy val onSubmit =
    routes.TotalAmountPensionPaymentsController.onSubmit(srn, index, NormalMode)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPSRSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPSRSubmissionService)
    when(mockPSRSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "TotalAmountPensionPaymentsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[MoneyView]
        .apply(
          viewModel(
            srn,
            index,
            memberDetails.fullName,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, TotalAmountPensionPaymentsPage(srn, index), money, userAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              index,
              memberDetails.fullName,
              form(injected[MoneyFormProvider]).fill(money),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
