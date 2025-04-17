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

import services.{PsrSubmissionService, SaveService}
import models.ConditionalYesNo._
import play.api.mvc.Call
import models.SchemeHoldLandProperty.Transfer
import play.api.inject.bind
import views.html.CheckYourAnswersView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec
import viewmodels.models.SectionJourneyStatus

class LandOrPropertyCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockSaveService: SaveService = mock[SaveService]
  private val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockSaveService)
    MockSaveService.save()
  }

  private val index = refineMV[OneTo5000](1)
  private val page = 1

  private def onPageLoad(mode: Mode): Call = routes.LandOrPropertyCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode): Call = routes.LandOrPropertyCYAController.onSubmit(srn, index, mode)

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
    .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index), postcodeLookup)
    .unsafeSet(AddressLookupResultsPage(srn, index), List(address, address, address))
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.yes[String, String]("landRegistryTitleNumber"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)

  private val incompleteUserAnswers = filledUserAnswers
    .unsafeSet(LandOrPropertyProgress(srn, index),
        SectionJourneyStatus.InProgress(
          controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
            .onPageLoad(srn, refineMV(1), NormalMode)
            .url
        )
    )

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
          .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
          redirectToPage(
            call = onPageLoad(mode),
            page =
              controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, page, mode),
            userAnswers = incompleteUserAnswers,
            previousUserAnswers = emptyUserAnswers,
            mockSaveService = Some(mockSaveService)
          )
//            .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
//            .after {
//              MockSaveService.capture(userAnswersCaptor)
//              userAnswersCaptor.getValue.get(LandOrPropertyHeldPage(srn)) mustEqual Some(true)
//            }
            .withName(s"redirect to list page when in $mode mode and incomplete data")
        )


      act.like(
        redirectToPage(
          call = onSubmit(mode),
          page =
            controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, page, NormalMode),
          userAnswers = filledUserAnswers,
          previousUserAnswers = emptyUserAnswers,
          mockSaveService = Some(mockSaveService)
        ).before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after {
            MockSaveService.capture(userAnswersCaptor)
            userAnswersCaptor.getValue.get(LandOrPropertyHeldPage(srn)) mustEqual Some(true)
          }
          .withName(s"set LandOrPropertyHeldPage to true when submitting in $mode")
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
          verify(mockPsrSubmissionService, never()).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to land or property list")
    )
  }
}
