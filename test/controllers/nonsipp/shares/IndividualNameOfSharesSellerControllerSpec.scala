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

package controllers.nonsipp.shares

import pages.nonsipp.shares.IndividualNameOfSharesSellerPage
import views.html.TextInputView
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import controllers.nonsipp.shares.IndividualNameOfSharesSellerController._
import config.RefinedTypes._
import controllers.ControllerBaseSpec

class IndividualNameOfSharesSellerControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)

  private lazy val onPageLoad =
    routes.IndividualNameOfSharesSellerController.onPageLoad(srn, index, NormalMode)

  private lazy val onSubmit =
    routes.IndividualNameOfSharesSellerController.onSubmit(srn, index, NormalMode)

  "IndividualNameOfSharesSellerController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, IndividualNameOfSharesSellerPage(srn, index), "test") {
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
