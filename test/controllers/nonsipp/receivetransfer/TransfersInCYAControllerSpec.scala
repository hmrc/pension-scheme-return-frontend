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

package controllers.nonsipp.receivetransfer

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.receivetransfer.TransfersInCYAController._
import eu.timepit.refined.refineMV
import models.{NormalMode, PensionSchemeType}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.receivetransfer._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import viewmodels.models.SectionCompleted
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class TransfersInCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad = routes.TransfersInCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.TransfersInCYAController.onSubmit(srn, index, NormalMode)

  private val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val pensionSchemeType = PensionSchemeType.Other("other")

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(TransfersInSectionCompleted(srn, index, secondaryIndex), SectionCompleted)
    .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)
    .unsafeSet(TransferringSchemeTypePage(srn, index, secondaryIndex), pensionSchemeType)
    .unsafeSet(TotalValueTransferPage(srn, index, secondaryIndex), money)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index, secondaryIndex), localDate)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index, secondaryIndex), true)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "TransfersInCYAController" - {

    val journeys = List(
      TransfersInCYA(
        secondaryIndex,
        transferringSchemeName,
        pensionSchemeType,
        money,
        localDate,
        transferIncludeAsset = true
      )
    )

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, journeys, NormalMode)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
