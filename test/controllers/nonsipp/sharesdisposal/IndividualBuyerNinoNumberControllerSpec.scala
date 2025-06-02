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

package controllers.nonsipp.sharesdisposal

import views.html.ConditionalYesNoPageView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.{IndividualBuyerNinoNumberPage, SharesIndividualBuyerNamePage}
import uk.gov.hmrc.domain.Nino
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec
import utils.IntUtils.toInt
import controllers.nonsipp.sharesdisposal.IndividualBuyerNinoNumberController._

class IndividualBuyerNinoNumberControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswersWithIndividualName =
    defaultUserAnswers.unsafeSet(SharesIndividualBuyerNamePage(srn, index, disposalIndex), individualName)

  private val conditionalNo: ConditionalYesNo[String, Nino] = ConditionalYesNo.no("reason")

  "Share disposal - IndividualBuyerNinoNumberController" - {

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
