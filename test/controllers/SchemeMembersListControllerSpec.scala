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

package controllers

import config.Refined.OneTo99
import controllers.SchemeMembersListController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.MemberDetailsPage
import views.html.ListView

class SchemeMembersListControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad = routes.SchemeMembersListController.onPageLoad(srn, 1)
  lazy val onSubmit = routes.SchemeMembersListController.onSubmit(srn, 1)

  private val userAnswersWithMembersDetails = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  private val userAnswersWith99MembersDetails =
    (1 to 99).foldLeft(defaultUserAnswers)(
      (ua, i) => ua.unsafeSet(MemberDetailsPage(srn, refineV[OneTo99](i).value), memberDetails)
    )

  "SchemeMembersListController" should {
    act.like(
      renderView(onPageLoad, userAnswersWithMembersDetails)(
        implicit app =>
          implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(srn, 1, NormalMode, List(memberDetails.fullName))
            )
      )
    )

    act.like(
      redirectToPage(
        onPageLoad,
        routes.MemberDetailsController.onPageLoad(srn, refineMV(1)),
        defaultUserAnswers
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      redirectToPage(
        onSubmit,
        routes.MemberDetailsController.onPageLoad(srn, refineMV(2)),
        userAnswersWithMembersDetails,
        "value" -> "true"
      )
    )

    "when user answers has 99 members" should {
      act.like(
        redirectToPage(
          onSubmit,
          routes.UnauthorisedController.onPageLoad,
          userAnswersWith99MembersDetails
        )
      )
    }

    act.like(redirectNextPage(onSubmit, "value" -> "false"))
    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
