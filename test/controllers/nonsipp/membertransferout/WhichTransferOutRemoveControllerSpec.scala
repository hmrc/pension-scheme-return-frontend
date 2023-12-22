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

package controllers.nonsipp.membertransferout

import controllers.ControllerBaseSpec
import controllers.nonsipp.membertransferout.WhichTransferOutRemoveController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NameDOB
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePage, WhenWasTransferMadePage}
import views.html.ListRadiosView

import java.time.LocalDate

class WhichTransferOutRemoveControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhichTransferOutRemoveController.onPageLoad(srn, refineMV(1))
  private lazy val onSubmit = routes.WhichTransferOutRemoveController.onSubmit(srn, refineMV(1))

  private val memberDetail1: NameDOB = nameDobGen.sample.value
  private val memberDetail2: NameDOB = nameDobGen.sample.value
  private val memberDetailsMap: Map[Int, (String, LocalDate)] =
    Map(0 -> (receivingSchemeName, localDate), 1 -> (receivingSchemeName + "2", localDate))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(WhenWasTransferMadePage(srn, refineMV(1), refineMV(1)), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, refineMV(1), refineMV(1)), receivingSchemeName)
    .unsafeSet(WhenWasTransferMadePage(srn, refineMV(1), refineMV(2)), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, refineMV(1), refineMV(2)), receivingSchemeName + "2")

  private val userAnswersWithOneContributionOnly = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(WhenWasTransferMadePage(srn, refineMV(1), refineMV(1)), localDate)
    .unsafeSet(ReceivingSchemeNamePage(srn, refineMV(1), refineMV(1)), receivingSchemeName)

  "WhichTransferOutRemoveController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, refineMV(1), memberDetail1.fullName, memberDetailsMap)
        )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        routes.RemoveTransferOutController.onPageLoad(srn, refineMV(1), refineMV(1)),
        userAnswersWithOneContributionOnly
      ).withName("should redirect to RemoveTransferOut page when only one contribution")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.RemoveTransferOutController.onPageLoad(srn, refineMV(1), refineMV(1)),
        userAnswers,
        "value" -> "1"
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
