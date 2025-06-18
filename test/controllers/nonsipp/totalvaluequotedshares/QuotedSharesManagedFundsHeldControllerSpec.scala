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

package controllers.nonsipp.totalvaluequotedshares

import services.{PsrSubmissionService, SchemeDateService}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.YesNoPageView
import forms.YesNoPageFormProvider
import models.{DateRange, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import controllers.nonsipp.totalvaluequotedshares.QuotedSharesManagedFundsHeldController.{form, viewModel}
import pages.nonsipp.totalvaluequotedshares.QuotedSharesManagedFundsHeldPage
import org.mockito.Mockito._

class QuotedSharesManagedFundsHeldControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.QuotedSharesManagedFundsHeldController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.QuotedSharesManagedFundsHeldController.onSubmit(srn, NormalMode)
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private val schemeDatePeriod: DateRange = dateRangeGen.sample.value
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    setSchemeDate(Some(schemeDatePeriod))
  }

  def setSchemeDate(date: Option[DateRange]): Unit =
    when(mockSchemeDateService.schemeDate(any())(using any())).thenReturn(date)

  "QuotedSharesManagedFundsHeldController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider], schemeDatePeriod),
          viewModel(srn, schemeName, NormalMode, schemeDatePeriod)
        )
    })

    act.like(renderPrePopView(onPageLoad, QuotedSharesManagedFundsHeldPage(srn), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], schemeDatePeriod).fill(true),
            viewModel(srn, schemeName, NormalMode, schemeDatePeriod)
          )
    })

    act.like(
      journeyRecoveryPage(onPageLoad)
        .withName("onPageLoad redirect to journey recovery page when scheme date not found")
        .before(setSchemeDate(None))
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after {
          verify(mockPsrSubmissionService, never).submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
          reset(mockPsrSubmissionService)
        }
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after {
          verify(mockPsrSubmissionService, times(1))
            .submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
          reset(mockPsrSubmissionService)
        }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      journeyRecoveryPage(onSubmit)
        .withName("onSubmit redirect to journey recovery page when scheme date not found")
        .before(setSchemeDate(None))
    )
  }
}
