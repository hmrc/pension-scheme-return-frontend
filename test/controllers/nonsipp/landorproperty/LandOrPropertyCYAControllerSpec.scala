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

package controllers.nonsipp.landorproperty

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.ConditionalYesNo._
import models.SchemeHoldLandProperty.Transfer
import models.{CheckOrChange, ConditionalYesNo}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.landorproperty._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.CheckYourAnswersView

class LandOrPropertyCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.LandOrPropertyCYAController.onPageLoad(srn, index, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) = routes.LandOrPropertyCYAController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, index), true)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.yes[String, String]("landRegistryTitleNumber"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)
    .unsafeSet(LandOrPropertyAddressLookupPage(srn, index), address)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index), false)

  "LandOrPropertyCYAController" - {
    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            LandOrPropertyCYAController.viewModel(
              ViewModelParameters(
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
                checkOrChange = checkOrChange
              )
            )
          )
        }.withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
