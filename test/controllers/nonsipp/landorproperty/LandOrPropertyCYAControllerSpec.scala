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

package controllers.nonsipp.landorproperty

import services.PsrSubmissionService
import models.ConditionalYesNo._
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.Refined.OneTo5000
import models.SchemeHoldLandProperty.Transfer

class LandOrPropertyCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)
  private val page = 1

  private def onPageLoad(mode: Mode) =
    routes.LandOrPropertyCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) = routes.LandOrPropertyCYAController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.LandOrPropertyCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.LandOrPropertyCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, index), true)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.yes[String, String]("landRegistryTitleNumber"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index), false)

  "LandOrPropertyCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            LandOrPropertyCYAController.viewModel(
              srn = srn,
              index = index,
              schemeName = schemeName,
              landOrPropertyInUk = true,
              landRegistryTitleNumber = ConditionalYesNo(Right("landRegistryTitleNumber")),
              holdLandProperty = Transfer,
              landOrPropertyAcquire = None,
              landOrPropertyTotalCost = money,
              landPropertyIndependentValuation = None,
              receivedLandType = None,
              recipientName = None,
              recipientDetails = None,
              recipientReasonNoDetails = None,
              landOrPropertySellerConnectedParty = None,
              landOrPropertyResidential = false,
              landOrPropertyLease = false,
              landOrPropertyTotalIncome = money,
              addressLookUpPage = address,
              leaseDetails = None,
              mode = mode,
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct ${mode.toString} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }

  "LandOrPropertyCYAController in view only mode" - {

    val currentUserAnswers = filledUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = filledUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          LandOrPropertyCYAController.viewModel(
            srn = srn,
            index = index,
            schemeName = schemeName,
            landOrPropertyInUk = true,
            landRegistryTitleNumber = ConditionalYesNo(Right("landRegistryTitleNumber")),
            holdLandProperty = Transfer,
            landOrPropertyAcquire = None,
            landOrPropertyTotalCost = money,
            landPropertyIndependentValuation = None,
            receivedLandType = None,
            recipientName = None,
            recipientDetails = None,
            recipientReasonNoDetails = None,
            landOrPropertySellerConnectedParty = None,
            landOrPropertyResidential = false,
            landOrPropertyLease = false,
            landOrPropertyTotalIncome = money,
            addressLookUpPage = address,
            leaseDetails = None,
            ViewOnlyMode,
            viewOnlyUpdated = false,
            optYear = Some(yearString),
            optCurrentVersion = Some(submissionNumberTwo),
            optPreviousVersion = Some(submissionNumberOne),
            compilationOrSubmissionDate = Some(submissionDateTwo)
          )
        )
      }
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to land or property list")
    )
  }
}
