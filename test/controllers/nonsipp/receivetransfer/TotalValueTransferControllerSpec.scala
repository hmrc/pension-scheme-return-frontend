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

import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import controllers.nonsipp.receivetransfer.TotalValueTransferController._
import views.html.MoneyView
import pages.nonsipp.receivetransfer.{TotalValueTransferPage, TransferringSchemeNamePage}
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode
import viewmodels.models.SectionCompleted
import config.RefinedTypes._
import controllers.ControllerBaseSpec

class TotalValueTransferControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)

  private lazy val onPageLoad =
    routes.TotalValueTransferController.onPageLoad(srn, index, secondaryIndex, NormalMode)

  private lazy val onSubmit =
    routes.TotalValueTransferController.onSubmit(srn, index, secondaryIndex, NormalMode)

  "TotalValueTransferController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[MoneyView]
        .apply(
          form(injected[MoneyFormProvider]),
          viewModel(
            srn,
            index,
            secondaryIndex,
            memberDetails.fullName,
            transferringSchemeName,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, TotalValueTransferPage(srn, index, secondaryIndex), money, userAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            form(injected[MoneyFormProvider]).fill(money),
            viewModel(
              srn,
              index,
              secondaryIndex,
              memberDetails.fullName,
              transferringSchemeName,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
