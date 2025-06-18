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

package controllers.nonsipp.membersurrenderedbenefits

import services.PsrSubmissionService
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import play.api.libs.json.JsPath
import forms.YesNoPageFormProvider
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsPage
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsController._

import scala.concurrent.Future

class SurrenderedBenefitsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.SurrenderedBenefitsController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.SurrenderedBenefitsController.onSubmit(srn, NormalMode)
  private lazy val unauthorisedSurrenders = "https://www.gov.uk/hmrc-internal-manuals/pensions-tax-manual/ptm133300"

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "SurrenderedBenefitsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, unauthorisedSurrenders, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, SurrenderedBenefitsPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]).fill(true),
          viewModel(srn, schemeName, unauthorisedSurrenders, NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        defaultUserAnswers,
        Some(JsPath \ "membersPayments" \ "surrenderMade"),
        "value" -> "true"
      )
    )

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
