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

package controllers.nonsipp.loansmadeoroutstanding

import controllers.nonsipp.loansmadeoroutstanding.RecipientSponsoringEmployerConnectedPartyController._
import play.api.mvc.Call
import views.html.RadioListView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models._
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import pages.nonsipp.loansmadeoroutstanding._
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class RecipientSponsoringEmployerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private val subject = IdentitySubject.LoanRecipient

  lazy val onPageLoad: Call =
    routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit: Call = routes.RecipientSponsoringEmployerConnectedPartyController.onSubmit(srn, index, NormalMode)

  val userAnswersWithCompanyName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn, index), companyName)

  val userAnswersWithPartnershipName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKPartnership)
      .unsafeSet(PartnershipRecipientNamePage(srn, index), partnershipName)

  val userAnswersWithOtherName: UserAnswers =
    defaultUserAnswers
      .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Other)
      .unsafeSet(
        OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient),
        RecipientDetails(otherName, "test description")
      )

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
