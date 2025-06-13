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

package controllers.nonsipp.sharesdisposal

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.TextInputView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.CompanyBuyerNamePage
import forms.TextFormProvider
import models.NormalMode
import controllers.nonsipp.sharesdisposal.CompanyNameOfSharesBuyerController._

class CompanyNameOfSharesBuyerControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val secondaryIndex = 1
  private lazy val onPageLoad =
    routes.CompanyNameOfSharesBuyerController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.CompanyNameOfSharesBuyerController.onSubmit(srn, index, secondaryIndex, NormalMode)

  private val companyNameOfSharesBuyer = "test"

  "CompanyNameOfSharesBuyerController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, secondaryIndex, NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, CompanyBuyerNamePage(srn, index, secondaryIndex), companyNameOfSharesBuyer) {
        implicit app => implicit request =>
          injected[TextInputView]
            .apply(
              form(injected[TextFormProvider]).fill(companyNameOfSharesBuyer),
              viewModel(srn, index, secondaryIndex, NormalMode)
            )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> companyNameOfSharesBuyer))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> companyNameOfSharesBuyer))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
