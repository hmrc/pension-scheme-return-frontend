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

package controllers.nonsipp.moneyborrowed

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.TextAreaView
import utils.IntUtils.given
import controllers.nonsipp.moneyborrowed.LenderNameController._
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.moneyborrowed.LenderNamePage

class LenderNameControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val multipleLenderNames = "Lee Lewis, John Todd, Jack Taylor"

  "LenderNameController" - {

    lazy val onPageLoad = routes.LenderNameController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.LenderNameController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, LenderNamePage(srn, index), lenderName) { implicit app => implicit request =>
      val preparedForm = form(injected[TextFormProvider]).fill(lenderName)
      injected[TextAreaView].apply(preparedForm, viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, LenderNamePage(srn, index), multipleLenderNames) {
      implicit app => implicit request =>
        val preparedForm = form(injected[TextFormProvider]).fill(multipleLenderNames)
        injected[TextAreaView].apply(preparedForm, viewModel(srn, index, NormalMode))
    }.withName("renderPrePopView with multiple names"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> lenderName))

    act.like(
      saveAndContinue(onSubmit, "value" -> multipleLenderNames)
        .withName("saveAndContinue with multiple names")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
