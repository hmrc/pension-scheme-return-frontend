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

package controllers.nonsipp.otherassetsheld

import WhatIsOtherAssetController._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage
import views.html.TextAreaView

class WhatIsOtherAssetControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhatIsOtherAssetController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.WhatIsOtherAssetController.onSubmit(srn, refineMV(1), NormalMode)

  "WhatIsOtherAssetController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[TextAreaView].apply(
        form(injected[TextFormProvider]),
        viewModel(srn, refineMV(1), NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, WhatIsOtherAssetPage(srn, refineMV(1)), "test text", defaultUserAnswers) {
      implicit app => implicit request =>
        injected[TextAreaView].apply(
          form(injected[TextFormProvider]).fill("test text"),
          viewModel(srn, refineMV(1), NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, defaultUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(invalidForm(onSubmit, defaultUserAnswers))
  }
}
