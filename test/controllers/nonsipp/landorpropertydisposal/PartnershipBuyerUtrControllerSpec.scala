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

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.ConditionalYesNoPageView
import pages.nonsipp.landorpropertydisposal.{PartnershipBuyerNamePage, PartnershipBuyerUtrPage}
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import controllers.nonsipp.landorpropertydisposal.PartnershipBuyerUtrController._

class PartnershipBuyerUtrControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.PartnershipBuyerUtrController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.PartnershipBuyerUtrController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  val userAnswersCompanyName: UserAnswers =
    defaultUserAnswers.unsafeSet(PartnershipBuyerNamePage(srn, index, disposalIndex), companyName)

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "PartnershipBuyerUtrController" - {

    act.like(renderView(onPageLoad, userAnswersCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, disposalIndex, NormalMode, companyName)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PartnershipBuyerUtrPage(srn, index, disposalIndex),
        conditionalNo,
        userAnswersCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, disposalIndex, NormalMode, companyName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value))

    act.like(invalidForm(onSubmit, userAnswersCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
