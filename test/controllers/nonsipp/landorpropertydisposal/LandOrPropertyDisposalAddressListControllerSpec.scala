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

package controllers.nonsipp.landorpropertydisposal

import play.api.mvc.Call
import controllers.ControllerBaseSpec
import views.html.ListRadiosView
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalProgress
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListController._
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyCompleted, LandOrPropertyProgress}

class LandOrPropertyDisposalAddressListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.LandOrPropertyDisposalAddressListController.onPageLoad(srn, page = 1)
  private lazy val onSubmit = routes.LandOrPropertyDisposalAddressListController.onSubmit(srn, page = 1)

  private val address1 = addressGen.sample.value
  private val address2 = addressGen.sample.value

  private val addresses = List(LandOrPropertyData(refineMV(1), address1), LandOrPropertyData(refineMV(2), address2))

  private val userAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(1)), address1)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(2)), address2)
    .unsafeSet(LandOrPropertyCompleted(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(LandOrPropertyCompleted(srn, refineMV(2)), SectionCompleted)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(1)), SectionJourneyStatus.Completed)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(2)), SectionJourneyStatus.Completed)

  private val incompleteUserAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(1)), address1)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(2)), address2)
    .unsafeSet(LandOrPropertyCompleted(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(1)), SectionJourneyStatus.Completed)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(2)), SectionJourneyStatus.InProgress("some-url"))

  private val incompleteDisposalUserAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(1)), address1)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(2)), address2)
    .unsafeSet(LandOrPropertyCompleted(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(LandOrPropertyCompleted(srn, refineMV(2)), SectionCompleted)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(1)), SectionJourneyStatus.Completed)
    .unsafeSet(LandOrPropertyProgress(srn, refineMV(2)), SectionJourneyStatus.Completed)
    .unsafeSet(
      LandOrPropertyDisposalProgress(srn, refineMV(1), refineMV(1)),
      SectionJourneyStatus.InProgress("some-url")
    )

  "LandOrPropertyDisposalAddressListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, addresses, userAnswers))
    })

    act.like(renderView(onPageLoad, incompleteUserAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, page = 1, List(LandOrPropertyData(refineMV(1), address1)), userAnswers)
        )
    }.updateName(_ + " - hide incomplete disposals"))

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(redirectToPage(onSubmit, Call(GET, "some-url"), incompleteDisposalUserAnswers, "value" -> "1"))
    act.like(redirectNextPage(onSubmit, incompleteDisposalUserAnswers, "value" -> "2"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
