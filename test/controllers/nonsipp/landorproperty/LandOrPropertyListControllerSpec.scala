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
import org.mockito.Mockito.{never, _}
import models.ConditionalYesNo._
import controllers.ControllerBaseSpec
import views.html.ListView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.SectionCompleted
import eu.timepit.refined.api.Refined
import org.mockito.ArgumentMatchers.any
import controllers.nonsipp.landorproperty.LandOrPropertyListController._
import config.Refined.OneTo5000

class LandOrPropertyListControllerSpec extends ControllerBaseSpec {

  val indexOne: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)
  val indexTwo: Refined[Int, OneTo5000] = refineMV[OneTo5000](2)

  private val address1 = addressGen.sample.value
  private val address2 = addressGen.sample.value

  private val addresses = Map("0" -> address1, "1" -> address2)

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, indexOne), true)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, indexOne), address1)
    .unsafeSet(LandRegistryTitleNumberPage(srn, indexOne), ConditionalYesNo.yes[String, String]("some-number"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne), SchemeHoldLandProperty.Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, indexOne), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, indexOne), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, indexOne), true)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, indexOne), true)
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexOne), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, indexOne), true)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexOne), money)
    .unsafeSet(LandOrPropertyCompleted(srn, indexOne), SectionCompleted)
    .unsafeSet(LandPropertyInUKPage(srn, indexTwo), true)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, indexTwo), address2)
    .unsafeSet(LandRegistryTitleNumberPage(srn, indexTwo), ConditionalYesNo.yes[String, String]("some-number"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexTwo), SchemeHoldLandProperty.Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, indexTwo), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, indexTwo), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, indexTwo), true)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, indexTwo), true)
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexTwo), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, indexTwo), true)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexTwo), money)
    .unsafeSet(RemovePropertyPage(srn, indexTwo), true)
    .unsafeSet(LandOrPropertyHeldPage(srn), true)

  private val noUserAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyHeldPage(srn), false)
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private lazy val onPageLoad = routes.LandOrPropertyListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onLandOrPropertyHeldPageLoad = routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode)
  private lazy val onSubmitViewOnly = routes.LandOrPropertyListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.LandOrPropertyListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.LandOrPropertyListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val page = 1

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  "LandOrPropertyListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          1,
          NormalMode,
          addresses,
          schemeName
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode),
        completedUserAnswers.remove(LandOrPropertyCompleted(srn, indexOne)).get
      ).withName("Incomplete Journey")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        onLandOrPropertyHeldPageLoad,
        defaultUserAnswers
      ).withName("Not Started Journey")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "LandOrPropertyListController in view only mode" - {
    val currentUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = false,
      year = yearString,
      currentVersion = submissionNumberTwo,
      previousVersion = submissionNumberOne,
      compilationOrSubmissionDate = Some(submissionDateTwo)
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              addresses,
              schemeName,
              Some(viewOnlyViewModel)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(IsLesseeConnectedPartyPage(srn, indexOne), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(defaultUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              addresses,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true))
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = noUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn,
            page,
            mode = ViewOnlyMode,
            Map(),
            schemeName,
            viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true))
          )
        )
      }.withName("OnPageLoadViewOnly renders ok with no land or property")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

    act.like(
      redirectToPage(
        onPreviousViewOnly,
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }
}
