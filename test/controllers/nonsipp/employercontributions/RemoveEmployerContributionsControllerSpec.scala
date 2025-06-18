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

import controllers.nonsipp.employercontributions.RemoveEmployerContributionsController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.NameDOB
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions.{EmployerNamePage, TotalEmployerContributionPage}
import services.PsrSubmissionService
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class RemoveEmployerContributionsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.RemoveEmployerContributionsController.onPageLoad(srn, 1, 1)
  private lazy val onSubmit = routes.RemoveEmployerContributionsController.onSubmit(srn, 1, 1)

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, 2), memberDetails)
    .unsafeSet(TotalEmployerContributionPage(srn, 1, 1), money)
    .unsafeSet(EmployerNamePage(srn, 1, 1), employerName)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveEmployerContributionsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, 1, 1, money, memberDetails.fullName, employerName)
      )
    })

    act.like(redirectToPage(onPageLoad, controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
