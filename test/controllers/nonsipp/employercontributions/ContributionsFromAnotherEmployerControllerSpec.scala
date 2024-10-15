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

import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions.ContributionsFromAnotherEmployerPage
import services.PsrSubmissionService
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when}
import config.RefinedTypes.{Max300, Max50}
import controllers.ControllerBaseSpec
import models.NormalMode
import controllers.nonsipp.employercontributions.ContributionsFromAnotherEmployerController.{form, viewModel}

import scala.concurrent.Future

class ContributionsFromAnotherEmployerControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    routes.ContributionsFromAnotherEmployerController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit =
    routes.ContributionsFromAnotherEmployerController.onSubmit(srn, index, secondaryIndex, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)

  override def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "ContributionsFromAnotherEmployerController" - {

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
      renderPrePopView(onPageLoad, ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), true, userAnswers) {
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
