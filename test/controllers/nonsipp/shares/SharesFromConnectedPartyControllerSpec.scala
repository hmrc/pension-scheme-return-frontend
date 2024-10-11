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

package controllers.nonsipp.shares

import pages.nonsipp.shares._
import controllers.nonsipp.shares.SharesFromConnectedPartyController._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import models.SchemeHoldShare.Acquisition
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.common.IdentityTypePage
import viewmodels.models.SectionCompleted

class SharesFromConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad = routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.SharesFromConnectedPartyController.onSubmit(srn, index, NormalMode)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"
  private val subject = IdentitySubject.SharesSeller

  "SharesFromConnectedPartyControllerSpec" - {
    val acquisitionUserAnswers = defaultUserAnswers
      .unsafeSet(SharesCompleted(srn, index), SectionCompleted)
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), Acquisition)
      .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
      .unsafeSet(CompanyNameOfSharesSellerPage(srn, index), companyName)

    act.like(renderView(onPageLoad, acquisitionUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, companyName, "", SchemeHoldShare.Acquisition, incomeTaxAct, NormalMode)
        )
    }.withName("Page renders for acquisition"))

    act.like(
      renderView(
        onPageLoad,
        defaultUserAnswers
          .unsafeSet(SharesCompleted(srn, index), SectionCompleted)
          .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
          .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)
          .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Transfer)
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(srn, index, "", companyName, SchemeHoldShare.Transfer, incomeTaxAct, NormalMode)
          )
      }.withName("Page renders for transfer")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        SharesFromConnectedPartyPage(srn, index),
        true,
        acquisitionUserAnswers
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKPartnership)
          .unsafeSet(PartnershipShareSellerNamePage(srn, index), partnershipName)
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, partnershipName, "", SchemeHoldShare.Acquisition, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, acquisitionUserAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, acquisitionUserAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, acquisitionUserAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, acquisitionUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
