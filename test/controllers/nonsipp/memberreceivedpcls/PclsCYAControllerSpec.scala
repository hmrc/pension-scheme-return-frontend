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

package controllers.nonsipp.memberreceivedpcls

import services.PsrSubmissionService
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import config.Refined._
import controllers.ControllerBaseSpec
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.inject.bind
import controllers.nonsipp.memberreceivedpcls.PclsCYAController._

import scala.concurrent.Future

class PclsCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private lazy val onPageLoad = routes.PclsCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.PclsCYAController.onSubmit(srn, index, NormalMode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private val lumpSumAmounts = pensionCommencementLumpSumGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index), lumpSumAmounts)

  "PclsCYAController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, lumpSumAmounts, NormalMode)
      )
    })

    act.like(
      redirectNextPage(onSubmit)
        .after(verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any()))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
