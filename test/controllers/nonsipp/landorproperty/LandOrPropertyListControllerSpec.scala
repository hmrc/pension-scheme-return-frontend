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

import models.ConditionalYesNo._
import controllers.ControllerBaseSpec
import views.html.ListView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode, SchemeHoldLandProperty}
import eu.timepit.refined.api.Refined
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
    .unsafeSet(RemovePropertyPage(srn, indexOne), true)
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

  private lazy val onPageLoad = routes.LandOrPropertyListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onLandOrPropertyHeldPageLoad = routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode)

  "LandOrPropertyListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          1,
          NormalMode,
          addresses
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, indexTwo, NormalMode),
        completedUserAnswers.remove(LandOrPropertyTotalIncomePage(srn, indexTwo)).get
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
}
