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

package controllers.nonsipp.landorproperty

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import views.html.ConditionalYesNoPageView
import pages.nonsipp.landorproperty.{IndividualSellerNiPage, LandPropertyIndividualSellersNamePage}
import eu.timepit.refined.refineMV
import uk.gov.hmrc.domain.Nino
import forms.YesNoPageFormProvider
import controllers.nonsipp.landorproperty.IndividualSellerNiController._
import models.{ConditionalYesNo, NormalMode, UserAnswers}

class IndividualSellerNiControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad =
    controllers.nonsipp.landorproperty.routes.IndividualSellerNiController
      .onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.landorproperty.routes.IndividualSellerNiController
      .onSubmit(srn, index, NormalMode)

  val userAnswersWithIndividualName: UserAnswers =
    defaultUserAnswers.unsafeSet(LandPropertyIndividualSellersNamePage(srn, index), individualName)

  val conditionalNo: ConditionalYesNo[String, Nino] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Nino] = ConditionalYesNo.yes(nino)

  "IndividualSellerNiController" - {

    act.like(renderView(onPageLoad, userAnswersWithIndividualName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, individualName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IndividualSellerNiPage(srn, index),
        conditionalNo,
        userAnswersWithIndividualName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, individualName, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> nino.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> nino.value))

    act.like(invalidForm(onSubmit, userAnswersWithIndividualName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
