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

package controllers.nonsipp.sharesdisposal

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.RadioListView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.HowWereSharesDisposedPage
import forms.RadioListFormProvider
import models.{HowSharesDisposed, NormalMode}
import controllers.nonsipp.sharesdisposal.HowWereSharesDisposedController._
import pages.nonsipp.shares.CompanyNameRelatedSharesPage

class HowWereSharesDisposedControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val shareIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.HowWereSharesDisposedController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.HowWereSharesDisposedController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)

  "HowWereSharesDisposedController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereSharesDisposedPage(srn, shareIndex, disposalIndex),
        HowSharesDisposed.Sold,
        userAnswers
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowSharesDisposed.Sold),
            viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Sold)")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereSharesDisposedPage(srn, shareIndex, disposalIndex),
        HowSharesDisposed.Redeemed,
        userAnswers
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowSharesDisposed.Redeemed),
            viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Redeemed)")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereSharesDisposedPage(srn, shareIndex, disposalIndex),
        HowSharesDisposed.Transferred,
        userAnswers
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowSharesDisposed.Transferred),
            viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Transferred)")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereSharesDisposedPage(srn, shareIndex, disposalIndex),
        HowSharesDisposed.Other("test details"),
        userAnswers
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowSharesDisposed.Other("test details")),
            viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Other)")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "Sold"))
    act.like(redirectNextPage(onSubmit, "value" -> "Redeemed"))
    act.like(redirectNextPage(onSubmit, "value" -> "Transferred"))
    act.like(redirectNextPage(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "Sold"))
    act.like(saveAndContinue(onSubmit, "value" -> "Redeemed"))
    act.like(saveAndContinue(onSubmit, "value" -> "Transferred"))
    act.like(saveAndContinue(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
