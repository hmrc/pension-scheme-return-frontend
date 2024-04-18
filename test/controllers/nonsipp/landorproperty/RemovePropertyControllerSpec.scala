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

package controllers.nonsipp.landorproperty

import services.PsrSubmissionService
import models.ConditionalYesNo._
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import controllers.nonsipp.landorproperty.RemovePropertyController._
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.Refined.OneTo5000
import models.SchemeHoldLandProperty.Transfer

class RemovePropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.RemovePropertyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemovePropertyController.onSubmit(srn, index, NormalMode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, index), true)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.yes[String, String]("landRegistryTitleNumber"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index), false)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "RemovePropertyController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, NormalMode, addressLine1 = address.addressLine1)
        )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )
    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(invalidForm(onSubmit, filledUserAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
