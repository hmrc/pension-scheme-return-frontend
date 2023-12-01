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

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.employercontributions.EmployerContributionsCYAController._
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn, IdentityType, NormalMode}
import pages.nonsipp.employercontributions.{
  EmployerCompanyCrnPage,
  EmployerNamePage,
  EmployerTypeOfBusinessPage,
  TotalEmployerContributionPage
}
import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.CheckYourAnswersView

class EmployerContributionsCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)
  private val page = 1

  private val employerCYAs = List(
    EmployerCYA(secondaryIndex, employerName, IdentityType.UKCompany, Right(crn.value), money)
  )

  private lazy val onPageLoad =
    routes.EmployerContributionsCYAController.onPageLoad(srn, index, page, NormalMode)
  private lazy val onSubmit = routes.EmployerContributionsCYAController.onSubmit(srn, index, page, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(EmployerNamePage(srn, index, secondaryIndex), employerName)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.UKCompany)
    .unsafeSet(TotalEmployerContributionPage(srn, index, secondaryIndex), money)
    .unsafeSet(EmployerCompanyCrnPage(srn, index, secondaryIndex), ConditionalYesNo.yes[String, Crn](crn))

  "EmployerContributionsCYAController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, page, employerCYAs, NormalMode)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
