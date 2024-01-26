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

package controllers.nonsipp.shares

import config.Refined.Max5000
import controllers.nonsipp.shares.PartnershipNameOfSharesSellerController._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.shares.PartnershipShareSellerNamePage
import views.html.TextInputView

class PartnershipNameOfSharesSellerControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)

  private lazy val onPageLoad = routes.PartnershipNameOfSharesSellerController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.PartnershipNameOfSharesSellerController.onSubmit(srn, index, NormalMode)

  "PartnershipNameOfSharesSellerController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, PartnershipShareSellerNamePage(srn, index), "test") {
      implicit app => implicit request =>
        injected[TextInputView]
          .apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, index, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
