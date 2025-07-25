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

package controllers.nonsipp.loansmadeoroutstanding

import play.api.test.FakeRequest
import models.ConditionalYesNo._
import org.jsoup.Jsoup
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ConditionalYesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode, Security}
import pages.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanPage
import controllers.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanController._

class SecurityGivenForLoanControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private lazy val onPageLoad = routes.SecurityGivenForLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.SecurityGivenForLoanController.onSubmit(srn, index, NormalMode)

  private val conditionalYes: ConditionalYes[Security] = ConditionalYesNo.yes(security)

  "SecurityGivenForLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, SecurityGivenForLoanPage(srn, index), conditionalYes) {
      implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalYes.value),
            viewModel(srn, index, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> "1"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "Should render conditional text area with correct id" in {
      val application = applicationBuilder().build()
      running(application) {

        val request = FakeRequest(GET, onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual OK

        val html = contentAsString(result)
        val document = Jsoup.parse(html)
        val textArea = document.select("textarea")
        textArea.attr("id") mustEqual "value.yes"
      }
    }
  }
}
