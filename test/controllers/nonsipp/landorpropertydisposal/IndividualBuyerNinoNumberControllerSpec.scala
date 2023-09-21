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

package controllers.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorpropertydisposal.IndividualBuyerNinoNumberController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode}
import pages.nonsipp.landorpropertydisposal.{IndividualBuyerNinoNumberPage, LandOrPropertyIndividualBuyerNamePage}
import uk.gov.hmrc.domain.Nino
import views.html.ConditionalYesNoPageView

class IndividualBuyerNinoNumberControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  val userAnswersWithIndividualName =
    defaultUserAnswers.unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, index, disposalIndex), individualName)

  val conditionalNo: ConditionalYesNo[String, Nino] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Nino] = ConditionalYesNo.yes(nino)

  "IndividualBuyerNinoNumberController" - {

    act.like(renderView(onPageLoad, userAnswersWithIndividualName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, disposalIndex, individualName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IndividualBuyerNinoNumberPage(srn, index, disposalIndex),
        conditionalNo,
        userAnswersWithIndividualName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, disposalIndex, individualName, NormalMode)
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
