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

import pages.nonsipp.employercontributions.{
  EmployerContributionsProgress,
  EmployerNamePage,
  TotalEmployerContributionPage
}
import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.nonsipp.employercontributions.WhichEmployerContributionRemoveController._
import views.html.ListRadiosView
import utils.IntUtils.given
import forms.RadioListFormProvider
import models.{Money, NameDOB}
import viewmodels.models.SectionJourneyStatus
import config.RefinedTypes.Max50
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class WhichEmployerContributionRemoveControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.WhichEmployerContributionRemoveController.onPageLoad(srn, 1)
  private lazy val onSubmit = routes.WhichEmployerContributionRemoveController.onSubmit(srn, 1)

  private val memberDetail1: NameDOB = nameDobGen.sample.value
  private val memberDetail2: NameDOB = nameDobGen.sample.value
  private val memberDetailsMap: List[(Max50, Money, String)] =
    List((1, money, employerName), (2, money, employerName + "2"))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, 2), memberDetail2)
    .unsafeSet(TotalEmployerContributionPage(srn, 1, 1), money)
    .unsafeSet(EmployerNamePage(srn, 1, 1), employerName)
    .unsafeSet(EmployerContributionsProgress(srn, 1, 1), SectionJourneyStatus.Completed)
    .unsafeSet(TotalEmployerContributionPage(srn, 1, 2), money)
    .unsafeSet(EmployerNamePage(srn, 1, 2), employerName + "2")
    .unsafeSet(EmployerContributionsProgress(srn, 1, 2), SectionJourneyStatus.Completed)

  private val userAnswersWithOneContributionOnly = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, 2), memberDetail2)
    .unsafeSet(TotalEmployerContributionPage(srn, 1, 1), money)
    .unsafeSet(EmployerNamePage(srn, 1, 1), employerName)
    .unsafeSet(EmployerContributionsProgress(srn, 1, 1), SectionJourneyStatus.Completed)

  "WhichEmployerContributionRemoveController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView].apply(
        form(injected[RadioListFormProvider]),
        viewModel(srn, 1, memberDetail1.fullName, memberDetailsMap)
      )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        routes.RemoveEmployerContributionsController.onPageLoad(srn, 1, 1),
        userAnswersWithOneContributionOnly
      ).withName("should redirect to RemoveEmployerContributions page when only one contribution")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.RemoveEmployerContributionsController.onPageLoad(srn, 1, 1),
        userAnswers,
        "value" -> "1"
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
