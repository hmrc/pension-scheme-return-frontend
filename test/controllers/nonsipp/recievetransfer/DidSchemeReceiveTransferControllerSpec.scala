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

package controllers.nonsipp.recievetransfer

import controllers.nonsipp.receivetransfer.routes
import controllers.ControllerBaseSpec
import views.html.YesNoPageView
import pages.nonsipp.receivetransfer.DidSchemeReceiveTransferPage
import controllers.nonsipp.receivetransfer.DidSchemeReceiveTransferController.{form, viewModel}
import play.api.libs.json.JsPath
import forms.YesNoPageFormProvider
import models.NormalMode

class DidSchemeReceiveTransferControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.DidSchemeReceiveTransferController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.DidSchemeReceiveTransferController.onSubmit(srn, NormalMode)

  "DidSchemeReceiveTransferController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, DidSchemeReceiveTransferPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, schemeName, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(onSubmit, Some(JsPath \ "membersPayments" \ "schemeReceivedTransferIn"), "value" -> "true")
    )

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
