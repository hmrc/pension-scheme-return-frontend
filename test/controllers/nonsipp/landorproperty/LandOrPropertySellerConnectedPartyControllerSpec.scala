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

package controllers.nonsipp.landorproperty

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.LandOrPropertySellerConnectedPartyController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{IdentitySubject, IdentityType, NormalMode, UserAnswers}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.landorproperty.{LandOrPropertySellerConnectedPartyPage, LandPropertyIndividualSellersNamePage}
import views.html.YesNoPageView

class LandOrPropertySellerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad = routes.LandOrPropertySellerConnectedPartyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertySellerConnectedPartyController.onSubmit(srn, index, NormalMode)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"

  val userServicesWithIndividualName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller), IdentityType.Individual)
      .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index), individualName)

  "LandOrPropertySellerConnectedPartyController" - {

    act.like(renderView(onPageLoad, userServicesWithIndividualName) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, individualName, incomeTaxAct, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        LandOrPropertySellerConnectedPartyPage(srn, index),
        true,
        userServicesWithIndividualName
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, individualName, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userServicesWithIndividualName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
