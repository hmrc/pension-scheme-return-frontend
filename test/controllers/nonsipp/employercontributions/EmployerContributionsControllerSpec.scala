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

package controllers.nonsipp.employercontributions

import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import controllers.nonsipp.employercontributions.EmployerContributionsController.viewModel
import play.api.libs.json.JsPath
import forms.YesNoPageFormProvider
import models.NormalMode
import play.api.data.Form
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions.EmployerContributionsPage
import services.PsrSubmissionService
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class EmployerContributionsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.EmployerContributionsController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.EmployerContributionsController.onSubmit(srn, NormalMode)

  val form: Form[Boolean] = EmployerContributionsController.form(new YesNoPageFormProvider())

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "EmployerContributionsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(
        form,
        viewModel(srn, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, EmployerContributionsPage(srn), true) { implicit app => implicit request =>
      val preparedForm = form.fill(true)
      injected[YesNoPageView].apply(preparedForm, viewModel(srn, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(onSubmit, Some(JsPath \ "membersPayments" \ "employerContributionMade"), "value" -> "true")
    )

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
