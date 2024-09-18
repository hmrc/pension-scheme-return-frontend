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

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.PostcodeLookupView
import eu.timepit.refined.refineMV
import forms.AddressLookupFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import services.AddressService
import controllers.nonsipp.landorproperty.LandOrPropertyPostcodeLookupController._
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class LandOrPropertyPostcodeLookupControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.LandOrPropertyPostcodeLookupController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyPostcodeLookupController.onSubmit(srn, index, NormalMode)
  private implicit val mockAddressService: AddressService = mock[AddressService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[AddressService].toInstance(mockAddressService)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressService)
  }

  "LandOrPropertyPostcodeLookupController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[PostcodeLookupView].apply(form(injected[AddressLookupFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(onSubmit, "postcode" -> "ZZ1 1ZZ", "filter" -> "")
        .before(
          when(mockAddressService.postcodeLookup(any(), any())(any())).thenReturn(Future.successful(List(address)))
        )
        .after(verify(mockAddressService, times(1)).postcodeLookup(any(), any())(any()))
    )

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
