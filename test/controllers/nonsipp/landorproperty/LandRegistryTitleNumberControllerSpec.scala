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

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.LandRegistryTitleNumberController._
import eu.timepit.refined.refineMV
import forms._
import models.{ConditionalYesNo, NormalMode}
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, LandRegistryTitleNumberPage}
import views.html._

class LandRegistryTitleNumberControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private lazy val onPageLoad = routes.LandRegistryTitleNumberController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandRegistryTitleNumberController.onSubmit(srn, index, NormalMode)

  private val userAnswersWithLookUpPage =
    defaultUserAnswers.unsafeSet(LandOrPropertyAddressLookupPage(srn, index), address)

  "LandRegistryTitleNumberController" - {

    act.like(renderView(onPageLoad, userAnswersWithLookUpPage) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, address.addressLine1, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        LandRegistryTitleNumberPage(srn, index),
        ConditionalYesNo.yes[String, String](titleNumber),
        userAnswersWithLookUpPage
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(Right(titleNumber)),
            viewModel(srn, index, address.addressLine1, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswersWithLookUpPage, "value" -> "true", "value.yes" -> titleNumber))
    act.like(redirectNextPage(onSubmit, userAnswersWithLookUpPage, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswersWithLookUpPage, "value" -> "true", "value.yes" -> titleNumber))

    act.like(invalidForm(onSubmit, userAnswersWithLookUpPage))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
