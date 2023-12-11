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

package controllers.nonsipp.memberreceivedpcls

import controllers.ControllerBaseSpec
import controllers.nonsipp.memberreceivedpcls.PclsMemberListController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NameDOB, NormalMode, UserAnswers}
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberreceivedpcls.PclsMemberListPage
import views.html.TwoColumnsTripleAction

class PclsMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.PclsMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.PclsMemberListController.onSubmit(srn, page = 1, NormalMode)

  val userAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  "PclsMemberListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList = userAnswers.membersDetails(srn)

      injected[TwoColumnsTripleAction].apply(
        form(injected[YesNoPageFormProvider]),
        viewModel(
          srn,
          page = 1,
          NormalMode,
          memberList: List[NameDOB],
          userAnswers: UserAnswers
        )
      )
    })

    act.like(renderPrePopView(onPageLoad, PclsMemberListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        val memberList = userAnswers.membersDetails(srn)

        injected[TwoColumnsTripleAction]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page = 1,
              NormalMode,
              memberList: List[NameDOB],
              userAnswers: UserAnswers
            )
          )
    })

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}