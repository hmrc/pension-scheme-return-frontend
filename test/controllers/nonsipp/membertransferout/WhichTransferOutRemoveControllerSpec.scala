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

package controllers.nonsipp.membertransferout

import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.ListRadiosView
import utils.IntUtils.given
import forms.RadioListFormProvider
import models.NameDOB
import pages.nonsipp.membertransferout.{MemberTransferOutProgress, ReceivingSchemeNamePage, WhenWasTransferMadePage}
import config.RefinedTypes.Max5
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import controllers.nonsipp.membertransferout.WhichTransferOutRemoveController._
import viewmodels.models.SectionJourneyStatus

import java.time.LocalDate

class WhichTransferOutRemoveControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.WhichTransferOutRemoveController.onPageLoad(srn, 1)
  private lazy val onSubmit = routes.WhichTransferOutRemoveController.onSubmit(srn, 1)

  private val memberDetail1: NameDOB = nameDobGen.sample.value
  private val memberDetail2: NameDOB = nameDobGen.sample.value
  private val memberDetailsMap: List[(Max5, String, LocalDate)] =
    List((1, receivingSchemeName, localDate), (2, receivingSchemeName + "2", localDate))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, 2), memberDetail2)
    .unsafeSet(WhenWasTransferMadePage(srn, 1, 1), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, 1, 1), receivingSchemeName)
    .unsafeSet(MemberTransferOutProgress(srn, 1, 1), SectionJourneyStatus.Completed)
    .unsafeSet(WhenWasTransferMadePage(srn, 1, 2), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, 1, 2), receivingSchemeName + "2")
    .unsafeSet(MemberTransferOutProgress(srn, 1, 2), SectionJourneyStatus.Completed)

  private val userAnswersWithOneContributionOnly = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, 2), memberDetail2)
    .unsafeSet(WhenWasTransferMadePage(srn, 1, 1), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, 1, 1), receivingSchemeName)
    .unsafeSet(MemberTransferOutProgress(srn, 1, 1), SectionJourneyStatus.Completed)

  "WhichTransferOutRemoveController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, 1, memberDetail1.fullName, memberDetailsMap)
        )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        routes.RemoveTransferOutController.onPageLoad(srn, 1, 1),
        userAnswersWithOneContributionOnly
      ).withName("should redirect to RemoveTransferOut page when only one contribution")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.RemoveTransferOutController.onPageLoad(srn, 1, 1),
        userAnswers,
        "value" -> "1"
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
