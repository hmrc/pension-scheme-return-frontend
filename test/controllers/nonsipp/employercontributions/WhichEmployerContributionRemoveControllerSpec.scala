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

package controllers.nonsipp.employercontributions

import controllers.ControllerBaseSpec
import controllers.nonsipp.employercontributions.WhichEmployerContributionRemoveController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{Money, NameDOB}
import pages.nonsipp.employercontributions.{EmployerNamePage, TotalEmployerContributionPage}
import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.ListRadiosView

class WhichEmployerContributionRemoveControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhichEmployerContributionRemoveController.onPageLoad(srn, refineMV(1))
  private lazy val onSubmit = routes.WhichEmployerContributionRemoveController.onSubmit(srn, refineMV(1))

  private val memberDetail1: NameDOB = nameDobGen.sample.value
  private val memberDetail2: NameDOB = nameDobGen.sample.value
  private val memberDetailsMap: Map[Int, (Money, String)] =
    Map(0 -> (money, employerName), 1 -> (money, employerName + "2"))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(TotalEmployerContributionPage(srn, refineMV(1), refineMV(1)), money)
    .unsafeSet(EmployerNamePage(srn, refineMV(1), refineMV(1)), employerName)
    .unsafeSet(TotalEmployerContributionPage(srn, refineMV(1), refineMV(2)), money)
    .unsafeSet(EmployerNamePage(srn, refineMV(1), refineMV(2)), employerName + "2")

  private val userAnswersWithOneContributionOnly = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetail1)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetail2)
    .unsafeSet(TotalEmployerContributionPage(srn, refineMV(1), refineMV(1)), money)
    .unsafeSet(EmployerNamePage(srn, refineMV(1), refineMV(1)), employerName)

  "WhichEmployerContributionRemoveController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[ListRadiosView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, refineMV(1), memberDetail1.fullName, memberDetailsMap)
      )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        routes.RemoveEmployerContributionsController.onPageLoad(srn, refineMV(1), refineMV(1)),
        userAnswersWithOneContributionOnly
      ).withName("should redirect to RemoveEmployerContributions page when only one contribution")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.RemoveEmployerContributionsController.onPageLoad(srn, refineMV(1), refineMV(1)),
        userAnswers,
        "value" -> "1"
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
