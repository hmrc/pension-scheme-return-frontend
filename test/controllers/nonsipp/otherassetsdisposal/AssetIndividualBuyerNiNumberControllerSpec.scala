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

package controllers.nonsipp.otherassetsdisposal

import views.html.ConditionalYesNoPageView
import eu.timepit.refined.refineMV
import uk.gov.hmrc.domain.Nino
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode}
import pages.nonsipp.otherassetsdisposal.{AssetIndividualBuyerNiNumberPage, IndividualNameOfAssetBuyerPage}
import controllers.nonsipp.otherassetsdisposal.AssetIndividualBuyerNiNumberController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class AssetIndividualBuyerNiNumberControllerSpec extends ControllerBaseSpec {

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.AssetIndividualBuyerNiNumberController
      .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.AssetIndividualBuyerNiNumberController
      .onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  private val userAnswersWithIndividualName =
    defaultUserAnswers.unsafeSet(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), individualName)

  private val conditionalNo: ConditionalYesNo[String, Nino] = ConditionalYesNo.no("reason")

  "AssetIndividualBuyerNiNumberController" - {

    act.like(renderView(onPageLoad, userAnswersWithIndividualName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, assetIndex, disposalIndex, individualName, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex),
        conditionalNo,
        userAnswersWithIndividualName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, assetIndex, disposalIndex, individualName, NormalMode)
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
