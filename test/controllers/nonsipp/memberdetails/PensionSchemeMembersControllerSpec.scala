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

package controllers.nonsipp.memberdetails

import models.ManualOrUpload.{Manual, Upload}
import views.html.RadioListView
import forms.RadioListFormProvider
import controllers.nonsipp.memberdetails.PensionSchemeMembersController._
import controllers.nonsipp.memberdetails.routes
import controllers.ControllerBaseSpec

class PensionSchemeMembersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.PensionSchemeMembersController.onPageLoad(srn)
  private lazy val onSubmit = routes.PensionSchemeMembersController.onSubmit(srn)

  "PensionSchemeMembersController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, defaultSchemeDetails.schemeName)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "manual data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Manual.name))
    }

    "upload data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Upload.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
