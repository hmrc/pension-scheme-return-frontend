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

package controllers.nonsipp.otherassetsheld

import pages.nonsipp.otherassetsheld._
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.IsAssetTangibleMoveablePropertyController._
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec
import models.{NormalMode, UserAnswers}
import models.SchemeHoldAsset.Transfer

class IsAssetTangibleMoveablePropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad =
    routes.IsAssetTangibleMoveablePropertyController.onPageLoad(srn, index.value, NormalMode)
  private lazy val onSubmit = routes.IsAssetTangibleMoveablePropertyController.onSubmit(srn, index.value, NormalMode)
  private val incomeTaxAct = "https://www.gov.uk/hmrc-internal-manuals/pensions-tax-manual/ptm125100#IDAUURQB"

  private val prePopUserAnswersMissing: UserAnswers = defaultUserAnswers
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index1of5000), Transfer)
    .unsafeSet(CostOfOtherAssetPage(srn, index1of5000), money)

  private val prePopUserAnswersCompleted: UserAnswers = defaultUserAnswers
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index1of5000), Transfer)
    .unsafeSet(CostOfOtherAssetPage(srn, index1of5000), money)
    .unsafeSet(IncomeFromAssetPage(srn, index1of5000), money)

  "IsAssetTangibleMoveablePropertyControllerSpec" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, incomeTaxAct, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IsAssetTangibleMoveablePropertyPage(srn, index),
        true,
        defaultUserAnswers
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = prePopUserAnswersMissing,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value" -> "true"
      ).withName("Skip interim pages when interim answers are present and IncomeFromAsset is empty (PrePop)")
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.otherassetsheld.routes.WhyDoesSchemeHoldAssetsController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = prePopUserAnswersCompleted,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value" -> "true"
      ).withName("Don't skip interim pages when IncomeFromAsset is present (PrePop)")
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.otherassetsheld.routes.WhyDoesSchemeHoldAssetsController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = defaultUserAnswers,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value" -> "true"
      ).withName("Don't skip interim pages when interim answers are empty (PrePop)")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, defaultUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
