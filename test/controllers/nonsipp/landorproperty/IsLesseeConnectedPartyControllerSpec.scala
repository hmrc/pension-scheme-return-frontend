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
import views.html.YesNoPageView
import pages.nonsipp.landorproperty.{IsLesseeConnectedPartyPage, LandOrPropertyLeaseDetailsPage}
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.IsLesseeConnectedPartyController._

class IsLesseeConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.IsLesseeConnectedPartyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.IsLesseeConnectedPartyController.onSubmit(srn, index, NormalMode)

  val userAnswersWithLeaseName: UserAnswers =
    defaultUserAnswers.unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index), (leaseName, money, localDate))

  "IsLesseeConnectedPartyController" - {

    act.like(renderView(onPageLoad, userAnswersWithLeaseName) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, leaseName, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, IsLesseeConnectedPartyPage(srn, index), true, userAnswersWithLeaseName) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, index, leaseName, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswersWithLeaseName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
