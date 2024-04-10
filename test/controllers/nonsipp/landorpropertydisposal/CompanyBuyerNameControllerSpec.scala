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

package controllers.nonsipp.landorpropertydisposal

import controllers.nonsipp.landorpropertydisposal.CompanyBuyerNameController.{form, viewModel}
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.TextInputView
import pages.nonsipp.landorpropertydisposal.CompanyBuyerNamePage
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode

class CompanyBuyerNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)
  private lazy val onPageLoad = routes.CompanyBuyerNameController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.CompanyBuyerNameController.onSubmit(srn, index, disposalIndex, NormalMode)

  "CompanyBuyerNameController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, disposalIndex, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, CompanyBuyerNamePage(srn, index, disposalIndex), "test") {
      implicit app => implicit request =>
        injected[TextInputView]
          .apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, index, disposalIndex, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
