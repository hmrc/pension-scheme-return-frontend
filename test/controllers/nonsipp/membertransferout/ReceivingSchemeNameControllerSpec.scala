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

package controllers.nonsipp.membertransferout

import config.Refined.{OneTo300, OneTo5}
import controllers.ControllerBaseSpec
import views.html.TextInputViewWidth40
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage

class ReceivingSchemeNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo300](1)
  private val transferIndex = refineMV[OneTo5](1)
  private lazy val onPageLoad =
    controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
      .onPageLoad(srn, index, transferIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
      .onSubmit(srn, index, transferIndex, NormalMode)

  "ReceivingSchemeNameController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputViewWidth40].apply(
        ReceivingSchemeNameController.form(injected[TextFormProvider]),
        ReceivingSchemeNameController.viewModel(srn, index, transferIndex, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, ReceivingSchemeNamePage(srn, index, transferIndex), "test") {
      implicit app => implicit request =>
        injected[TextInputViewWidth40].apply(
          ReceivingSchemeNameController.form(injected[TextFormProvider]).fill("test"),
          ReceivingSchemeNameController.viewModel(srn, index, transferIndex, NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
