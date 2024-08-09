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

package controllers.nonsipp.membertransferout

import services.PsrSubmissionService
import config.Refined._
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models.{NormalMode, PensionSchemeType, ViewOnlyMode}
import pages.nonsipp.membertransferout._
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito._
import controllers.ControllerBaseSpec
import controllers.nonsipp.membertransferout.TransfersOutCYAController._

import scala.concurrent.Future

class TransfersOutCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private val page = 1
  private lazy val onPageLoad = routes.TransfersOutCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.TransfersOutCYAController.onSubmit(srn, index, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransfersOutCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.TransfersOutCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val pensionSchemeType = PensionSchemeType.Other("other")

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(TransfersOutSectionCompleted(srn, index, secondaryIndex), SectionCompleted)
    .unsafeSet(ReceivingSchemeNamePage(srn, index, secondaryIndex), receivingSchemeName)
    .unsafeSet(ReceivingSchemeTypePage(srn, index, secondaryIndex), pensionSchemeType)
    .unsafeSet(WhenWasTransferMadePage(srn, index, secondaryIndex), localDate)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  val journeys: List[TransfersOutCYA] = List(
    TransfersOutCYA(
      secondaryIndex,
      receivingSchemeName,
      pensionSchemeType,
      localDate
    )
  )

  "TransfersOutCYAController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, journeys, NormalMode, viewOnlyUpdated = true)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "TransfersOutCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(TransfersOutSectionCompleted(srn, index, secondaryIndex), SectionCompleted)
      .unsafeSet(ReceivingSchemeNamePage(srn, index, secondaryIndex), receivingSchemeName)
      .unsafeSet(ReceivingSchemeTypePage(srn, index, secondaryIndex), pensionSchemeType)
      .unsafeSet(WhenWasTransferMadePage(srn, index, secondaryIndex), localDate)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(TransfersOutSectionCompleted(srn, index, secondaryIndex), SectionCompleted)

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
        controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to TransferOutMemberListController page")
    )
  }

}
