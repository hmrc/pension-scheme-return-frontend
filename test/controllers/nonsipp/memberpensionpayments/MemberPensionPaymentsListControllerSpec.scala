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

package controllers.nonsipp.memberpensionpayments

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import forms.YesNoPageFormProvider
import models.{NameDOB, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito._
import pages.nonsipp.memberpensionpayments.MemberPensionPaymentsListPage
import eu.timepit.refined.refineMV

class MemberPensionPaymentsListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.MemberPensionPaymentsListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.MemberPensionPaymentsListController.onSubmit(srn, page = 1, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )
  val userAnswers: UserAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  "MemberPensionPaymentsListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberMap = userAnswers.membersDetails(srn)

      val maxIndex: Int = memberMap.keys
        .map(_.toInt)
        .maxOption
        .get

      val memberList: List[Option[NameDOB]] =
        (0 to maxIndex).toList.map { index =>
          val memberOption = memberMap.get(index.toString)
          memberOption match {
            case Some(member) => Some(member)
            case None => None
          }
        }

      injected[TwoColumnsTripleAction].apply(
        MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]),
        MemberPensionPaymentsListController.viewModel(
          srn,
          page = 1,
          NormalMode,
          memberList: List[Option[NameDOB]],
          userAnswers
        )
      )
    })

    act.like(renderPrePopView(onPageLoad, MemberPensionPaymentsListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        val memberMap = userAnswers.membersDetails(srn)

        val maxIndex: Int = memberMap.keys
          .map(_.toInt)
          .maxOption
          .get

        val memberList: List[Option[NameDOB]] =
          (0 to maxIndex).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }

        injected[TwoColumnsTripleAction]
          .apply(
            MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]).fill(true),
            MemberPensionPaymentsListController.viewModel(
              srn,
              page = 1,
              NormalMode,
              memberList: List[Option[NameDOB]],
              userAnswers
            )
          )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
