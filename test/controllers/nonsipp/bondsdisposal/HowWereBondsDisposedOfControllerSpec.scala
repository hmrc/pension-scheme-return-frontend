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

package controllers.nonsipp.bondsdisposal

import controllers.nonsipp.bondsdisposal.HowWereBondsDisposedOfController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.RadioListView
import utils.IntUtils.given
import forms.RadioListFormProvider
import models.{HowDisposed, NormalMode}
import pages.nonsipp.bondsdisposal.HowWereBondsDisposedOfPage

class HowWereBondsDisposedOfControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val bondIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.HowWereBondsDisposedOfController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.HowWereBondsDisposedOfController.onSubmit(srn, bondIndex, disposalIndex, NormalMode)

  "HowWereBondsDisposedOfController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, bondIndex, disposalIndex, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex),
        HowDisposed.Sold
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowDisposed.Sold),
            viewModel(srn, bondIndex, disposalIndex, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Sold)")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex),
        HowDisposed.Transferred
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowDisposed.Transferred),
            viewModel(srn, bondIndex, disposalIndex, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Transferred)")
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex),
        HowDisposed.Other("test details")
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowDisposed.Other("test details")),
            viewModel(srn, bondIndex, disposalIndex, NormalMode)
          )
      }.withName("return OK and the correct pre-populated view for a GET (Other)")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "Sold"))
    act.like(redirectNextPage(onSubmit, "value" -> "Transferred"))
    act.like(redirectNextPage(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "Sold"))
    act.like(saveAndContinue(onSubmit, "value" -> "Transferred"))
    act.like(saveAndContinue(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
