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

package controllers.nonsipp.memberreceivedpcls

import controllers.ControllerBaseSpec
import controllers.nonsipp.memberreceivedpcls.PensionCommencementLumpSumController.viewModel
import forms.YesNoPageFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.memberreceivedpcls.{Paths, PensionCommencementLumpSumPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.YesNoPageView

import scala.concurrent.Future

class PensionCommencementLumpSumControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.PensionCommencementLumpSumController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.PensionCommencementLumpSumController.onSubmit(srn, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private val form: Form[Boolean] = PensionCommencementLumpSumController.form(new YesNoPageFormProvider())

  "PensionCommencementLumpSumController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form, viewModel(srn, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, PensionCommencementLumpSumPage(srn), true) {
      implicit app => implicit request =>
        val preparedForm = form.fill(true)
        injected[YesNoPageView].apply(preparedForm, viewModel(srn, NormalMode))
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any())(any(), any(), any())
        })
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(saveAndContinue(onSubmit, Some(Paths.memberDetails \ "lumpSumReceived"), "value" -> "true"))

    act.like(invalidForm(onSubmit))
  }
}
