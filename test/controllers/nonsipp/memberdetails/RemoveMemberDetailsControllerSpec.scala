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

package controllers.nonsipp.memberdetails

import services.PsrSubmissionService
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NameDOB, NormalMode}
import controllers.nonsipp.memberdetails.RemoveMemberDetailsController.{form, viewModel}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.{MemberDetailsPage, NoNINOPage}
import org.mockito.Mockito.{reset, when}
import controllers.nonsipp.memberdetails.routes
import controllers.ControllerBaseSpec

import scala.concurrent.Future

class RemoveMemberDetailsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveMemberDetailsController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.RemoveMemberDetailsController.onSubmit(srn, refineMV(1), NormalMode)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(NoNINOPage(srn, refineMV(1)), "reason")
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .unsafeSet(NoNINOPage(srn, refineMV(2)), "reason")

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveMemberDetailsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider], nameDobGen.sample.value),
        viewModel(srn, refineMV(1), memberDetails, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
