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

package controllers.nonsipp.receivetransfer

import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.YesNoPageView
import pages.nonsipp.receivetransfer.{DidTransferIncludeAssetPage, TransferringSchemeNamePage}
import forms.YesNoPageFormProvider
import models.NormalMode
import config.RefinedTypes._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import controllers.nonsipp.receivetransfer.DidTransferIncludeAssetController._

class DidTransferIncludeAssetControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad =
    routes.DidTransferIncludeAssetController.onPageLoad(srn, index.value, secondaryIndex.value, NormalMode)
  private lazy val onSubmit =
    routes.DidTransferIncludeAssetController.onSubmit(srn, index.value, secondaryIndex.value, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)

  "DidTransferIncludeAssetController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, transferringSchemeName, memberDetails.fullName, index, secondaryIndex, NormalMode)
        )
    })

    act.like(renderPrePopView(onPageLoad, DidTransferIncludeAssetPage(srn, index, secondaryIndex), true, userAnswers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, transferringSchemeName, memberDetails.fullName, index, secondaryIndex, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
