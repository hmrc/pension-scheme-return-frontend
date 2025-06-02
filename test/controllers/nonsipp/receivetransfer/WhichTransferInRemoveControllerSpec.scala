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
import views.html.ListRadiosView
import pages.nonsipp.receivetransfer.{ReceiveTransferProgress, TotalValueTransferPage, TransferringSchemeNamePage}
import eu.timepit.refined.refineMV
import controllers.nonsipp.receivetransfer.WhichTransferInRemoveController._
import forms.RadioListFormProvider
import models.{Money, NameDOB}
import viewmodels.models.SectionJourneyStatus
import config.RefinedTypes.Max5
import controllers.ControllerBaseSpec

class WhichTransferInRemoveControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhichTransferInRemoveController.onPageLoad(srn, 1)
  private lazy val onSubmit = routes.WhichTransferInRemoveController.onSubmit(srn, 1)

  private val memberDetail1: NameDOB = nameDobGen.sample.value
  private val memberDetail2: NameDOB = nameDobGen.sample.value
  private val memberDetailsMap: List[(Max5, Money, String)] =
    List((refineMV(1), money, transferringSchemeName), (refineMV(2), money, transferringSchemeName + "2"))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(TotalValueTransferPage(srn, refineMV(1), refineMV(1)), money)
    .unsafeSet(TransferringSchemeNamePage(srn, refineMV(1), refineMV(1)), transferringSchemeName)
    .unsafeSet(ReceiveTransferProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
    .unsafeSet(TotalValueTransferPage(srn, refineMV(1), refineMV(2)), money)
    .unsafeSet(TransferringSchemeNamePage(srn, refineMV(1), refineMV(2)), transferringSchemeName + "2")
    .unsafeSet(ReceiveTransferProgress(srn, refineMV(1), refineMV(2)), SectionJourneyStatus.Completed)

  private val userAnswersWithOneContributionOnly = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(TotalValueTransferPage(srn, refineMV(1), refineMV(1)), money)
    .unsafeSet(TransferringSchemeNamePage(srn, refineMV(1), refineMV(1)), transferringSchemeName)
    .unsafeSet(ReceiveTransferProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)

  "WhichTransferInRemoveController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView].apply(
        form(injected[RadioListFormProvider]),
        viewModel(srn, refineMV(1), memberDetail1.fullName, memberDetailsMap)
      )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        routes.RemoveTransferInController.onPageLoad(srn, 1, 1),
        userAnswersWithOneContributionOnly
      ).withName("should redirect to RemoveTransferIn page when only one contribution")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.RemoveTransferInController.onPageLoad(srn, 1, 1),
        userAnswers,
        "value" -> "1"
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
