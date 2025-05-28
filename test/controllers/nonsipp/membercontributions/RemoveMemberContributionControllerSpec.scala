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

package controllers.nonsipp.membercontributions

import services.PsrSubmissionService
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NameDOB
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito._

import scala.concurrent.Future

class RemoveMemberContributionControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveMemberContributionController.onPageLoad(srn, 1)
  private lazy val onSubmit = routes.RemoveMemberContributionController.onSubmit(srn, 1)

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), money)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveMemberContributionController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        RemoveMemberContributionController.form(injected[YesNoPageFormProvider]),
        RemoveMemberContributionController.viewModel(srn, refineMV(1), money, memberDetails.fullName)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(
      continueNoSave(onSubmit, userAnswers, "value" -> "false")
        .after(verify(mockPsrSubmissionService, never).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
    )

    act.like(
      saveAndContinue(onSubmit, userAnswers, "value" -> "true")
        .after(
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
        )
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
