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

package controllers.nonsipp.receivetransfer

import services.PsrSubmissionService
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.toInt
import pages.nonsipp.receivetransfer._
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models.{NormalMode, PensionSchemeType, ViewOnlyMode}
import viewmodels.models.SectionCompleted
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito._
import config.RefinedTypes._
import controllers.ControllerBaseSpec
import controllers.nonsipp.receivetransfer.TransfersInCYAController._
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class TransfersInCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private val page = 1
  private lazy val onPageLoad = routes.TransfersInCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.TransfersInCYAController.onSubmit(srn, index, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransfersInCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.TransfersInCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

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
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private val journeys = List(
    TransfersInCYA(
      secondaryIndex,
      transferringSchemeName,
      pensionSchemeType,
      money,
      localDate,
      transferIncludeAsset = true
    )
  )

  "TransfersInCYAController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          memberDetails.fullName,
          index,
          journeys,
          NormalMode,
          viewOnlyUpdated = true
        )
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "TransfersInCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(TransfersInSectionCompleted(srn, index, secondaryIndex), SectionCompleted)
      .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)
      .unsafeSet(TransferringSchemeTypePage(srn, index, secondaryIndex), pensionSchemeType)
      .unsafeSet(TotalValueTransferPage(srn, index, secondaryIndex), money)
      .unsafeSet(WhenWasTransferReceivedPage(srn, index, secondaryIndex), localDate)
      .unsafeSet(DidTransferIncludeAssetPage(srn, index, secondaryIndex), true)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(TransfersInSectionCompleted(srn, index, secondaryIndex), SectionCompleted)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              memberDetails.fullName,
              index,
              journeys,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
      ).withName("Submit redirects to TransferReceivedMemberListController page")
    )
  }
}
