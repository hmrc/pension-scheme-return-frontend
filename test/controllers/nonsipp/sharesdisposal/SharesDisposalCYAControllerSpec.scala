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

package controllers.nonsipp.sharesdisposal

import config.Refined.{OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.{CheckMode, Mode, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class SharesDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private val shareIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  private def onPageLoad(mode: Mode) =
    routes.SharesDisposalCYAController.onPageLoad(srn, shareIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.SharesDisposalCYAController.onSubmit(srn, shareIndex, disposalIndex, mode)

  private val filledUserAnswers = defaultUserAnswers
  //TODO:

  "SharesDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
//      act.like(
//        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
//          injected[CheckYourAnswersView].apply(
//            viewModel(
//              srn,
//              //TODO:
//              mode
//            )
//          )
//        }.withName(s"render correct ${mode} view")
//      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${mode} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${mode} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${mode} mode")
      )
    }
  }
}
