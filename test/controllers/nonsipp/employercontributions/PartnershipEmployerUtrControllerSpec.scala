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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions.{EmployerNamePage, PartnershipEmployerUtrPage}
import views.html.ConditionalYesNoPageView
import controllers.nonsipp.employercontributions.PartnershipEmployerUtrController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import config.RefinedTypes.{Max300, Max50}
import controllers.ControllerBaseSpec

class PartnershipEmployerUtrControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
      .onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
      .onSubmit(srn, index, secondaryIndex, NormalMode)

  val userAnswersWithEmployerName: UserAnswers =
    defaultUserAnswers.unsafeSet(EmployerNamePage(srn, index, secondaryIndex), employerName)

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "PartnershipEmployerUtrController" - {

    act.like(renderView(onPageLoad, userAnswersWithEmployerName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, secondaryIndex, NormalMode, employerName))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PartnershipEmployerUtrPage(srn, index, secondaryIndex),
        conditionalNo,
        userAnswersWithEmployerName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, secondaryIndex, NormalMode, employerName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value))

    act.like(invalidForm(onSubmit, userAnswersWithEmployerName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
