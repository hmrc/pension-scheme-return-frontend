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

package controllers.nonsipp.memberdetails

import config.Refined.OneTo300
import controllers.ControllerBaseSpec
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.ManualOrUpload.{Manual, Upload}
import models.NormalMode
import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.ListView

class SchemeMembersListControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoadManual = routes.SchemeMembersListController.onPageLoad(srn, 1, Manual)
  lazy val onPageLoadUpload = routes.SchemeMembersListController.onPageLoad(srn, 1, Upload)
  lazy val onSubmitManual = routes.SchemeMembersListController.onSubmit(srn, 1, Manual)
  lazy val onSubmitUpload = routes.SchemeMembersListController.onSubmit(srn, 1, Upload)

  private val userAnswersWithMembersDetails = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  private val userAnswersWith300MembersDetails =
    (1 to 300).foldLeft(defaultUserAnswers)(
      (ua, i) => ua.unsafeSet(MemberDetailsPage(srn, refineV[OneTo300](i).value), memberDetails)
    )

  "SchemeMembersListController" - {
    "on Manual" - {
      act.like(
        renderView(onPageLoadManual, userAnswersWithMembersDetails)(
          implicit app =>
            implicit request =>
              injected[ListView].apply(
                form(injected[YesNoPageFormProvider], Manual),
                viewModel(srn, 1, Manual, NormalMode, List(memberDetails.fullName))
              )
        )
      )

      act.like(
        redirectToPage(
          onPageLoadManual,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          defaultUserAnswers
        )
      )

      act.like(journeyRecoveryPage(onPageLoadManual).updateName("onPageLoad" + _))

      act.like(
        redirectToPage(
          onSubmitManual,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          userAnswersWithMembersDetails,
          "value" -> "true"
        )
      )

      "when user answers has 300 members" - {
        act.like(
          redirectToPage(
            onSubmitManual,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          )
        )
      }

      act.like(redirectNextPage(onSubmitManual, "value" -> "false"))
      act.like(invalidForm(onSubmitManual))
      act.like(journeyRecoveryPage(onSubmitManual).updateName("onSubmit" + _))
    }

    "on Upload" - {
      act.like(
        renderView(onPageLoadUpload, userAnswersWithMembersDetails)(
          implicit app =>
            implicit request =>
              injected[ListView].apply(
                form(injected[YesNoPageFormProvider], Upload),
                viewModel(srn, 1, Upload, NormalMode, List(memberDetails.fullName))
              )
        )
      )

      act.like(
        redirectToPage(
          onPageLoadUpload,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          defaultUserAnswers
        )
      )

      act.like(journeyRecoveryPage(onPageLoadUpload).updateName("onPageLoad" + _))

      act.like(
        redirectToPage(
          onSubmitUpload,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          userAnswersWithMembersDetails,
          "value" -> "true"
        )
      )

      "when user answers has more than 300 members" - {
        act.like(
          redirectToPage(
            onSubmitUpload,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          )
        )
      }

      act.like(redirectNextPage(onSubmitUpload, "value" -> "false"))
      act.like(invalidForm(onSubmitUpload))
      act.like(journeyRecoveryPage(onSubmitUpload).updateName("onSubmit" + _))
    }
  }
}
