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

import controllers.nonsipp.loansmadeoroutstanding.RecipientSponsoringEmployerConnectedPartyController._
import controllers.ControllerBaseSpec
import forms.RadioListFormProvider
import models.{NormalMode, ReceivedLoanType, RecipientDetails, SponsoringOrConnectedParty, UserAnswers}
import pages.nonsipp.loansmadeoroutstanding.{
  CompanyRecipientNamePage,
  IndividualRecipientNamePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientNamePage,
  RecipientSponsoringEmployerConnectedPartyPage,
  WhoReceivedLoanPage
}
import views.html.RadioListView

class RecipientSponsoringEmployerConnectedPartyControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad = routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, NormalMode)
  lazy val onSubmit = routes.RecipientSponsoringEmployerConnectedPartyController.onSubmit(srn, NormalMode)

  val userAnswersWithCompanyName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(WhoReceivedLoanPage(srn), ReceivedLoanType.UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn), companyName)

  val userAnswersWithPartnershipName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(WhoReceivedLoanPage(srn), ReceivedLoanType.UKPartnership)
      .unsafeSet(PartnershipRecipientNamePage(srn), partnershipName)

  val userAnswersWithOtherName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(WhoReceivedLoanPage(srn), ReceivedLoanType.Other)
      .unsafeSet(OtherRecipientDetailsPage(srn), RecipientDetails(otherName, "test description"))

  "RecipientSponsoringEmployerConnectedParty Controller" - {

    act.like(renderView(onPageLoad, userAnswersWithCompanyName) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, companyName, NormalMode))
    }.updateName(_ + " company name"))

    act.like(renderView(onPageLoad, userAnswersWithPartnershipName) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, partnershipName, NormalMode))
    }.updateName(_ + " partnership name"))

    act.like(renderView(onPageLoad, userAnswersWithOtherName) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, otherName, NormalMode))
    }.updateName(_ + " other name"))

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithCompanyName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, companyName, NormalMode)
          )
      }.updateName(_ + " company name")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithPartnershipName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, partnershipName, NormalMode)
          )
      }.updateName(_ + " partnership name")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        RecipientSponsoringEmployerConnectedPartyPage(srn),
        SponsoringOrConnectedParty.Sponsoring,
        userAnswersWithOtherName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SponsoringOrConnectedParty.Sponsoring),
            viewModel(srn, otherName, NormalMode)
          )
      }.updateName(_ + " other name")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> SponsoringOrConnectedParty.Sponsoring.name))

    act.like(invalidForm(onSubmit, userAnswersWithCompanyName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
