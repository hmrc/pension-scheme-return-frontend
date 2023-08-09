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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.OneTo9999999
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.RecipientSponsoringEmployerConnectedPartyController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{NormalMode, ReceivedLoanType, RecipientDetails, SponsoringOrConnectedParty, UserAnswers}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import views.html.RadioListView

class RecipientSponsoringEmployerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  lazy val onPageLoad = routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit = routes.RecipientSponsoringEmployerConnectedPartyController.onSubmit(srn, index, NormalMode)

  val userAnswersWithCompanyName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index), ReceivedLoanType.UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn, index), companyName)

  val userAnswersWithPartnershipName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index), ReceivedLoanType.UKPartnership)
      .unsafeSet(PartnershipRecipientNamePage(srn, index), partnershipName)

  val userAnswersWithOtherName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index), ReceivedLoanType.Other)
      .unsafeSet(OtherRecipientDetailsPage(srn, index), RecipientDetails(otherName, "test description"))

  "RecipientSponsoringEmployerConnectedParty Controller" - {

    act.like(renderView(onPageLoad, userAnswersWithCompanyName) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, index, companyName, NormalMode))
    }.updateName(_ + " company name"))

    act.like(renderView(onPageLoad, userAnswersWithPartnershipName) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, index, partnershipName, NormalMode))
    }.updateName(_ + " partnership name"))

    act.like(renderView(onPageLoad, userAnswersWithOtherName) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, index, otherName, NormalMode))
    }.updateName(_ + " other name"))

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn, index),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithCompanyName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, index, companyName, NormalMode)
          )
      }.updateName(_ + " company name")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn, index),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithPartnershipName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, index, partnershipName, NormalMode)
          )
      }.updateName(_ + " partnership name")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn, index),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithOtherName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, index, otherName, NormalMode)
          )
      }.updateName(_ + " other name")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> SponsoringOrConnectedParty.Sponsoring.name))

    act.like(invalidForm(onSubmit, userAnswersWithCompanyName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
