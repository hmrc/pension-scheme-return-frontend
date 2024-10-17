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

package controllers.nonsipp.moneyborrowed

import views.html.TextAreaView
import eu.timepit.refined.refineMV
import controllers.nonsipp.moneyborrowed.WhySchemeBorrowedMoneyController._
import forms.TextFormProvider
import models.{NormalMode, UserAnswers}
import pages.nonsipp.moneyborrowed.{BorrowedAmountAndRatePage, LenderNamePage, WhySchemeBorrowedMoneyPage}
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class WhySchemeBorrowedMoneyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.WhySchemeBorrowedMoneyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.WhySchemeBorrowedMoneyController.onSubmit(srn, index, NormalMode)

  val populatedUserAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, index), lenderName)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index), amountBorrowed)

  "WhySchemeBorrowedMoneyController" - {

    act.like(renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      injected[TextAreaView].apply(
        form(injected[TextFormProvider]),
        viewModel(srn, index, NormalMode, schemeName, amountBorrowed._1.displayAs, lenderName)
      )
    })

    act.like(renderPrePopView(onPageLoad, WhySchemeBorrowedMoneyPage(srn, index), "test text", populatedUserAnswers) {
      implicit app => implicit request =>
        injected[TextAreaView].apply(
          form(injected[TextFormProvider]).fill("test text"),
          viewModel(srn, index, NormalMode, schemeName, amountBorrowed._1.displayAs, lenderName)
        )
    })

    act.like(redirectNextPage(onSubmit, populatedUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, populatedUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(invalidForm(onSubmit, populatedUserAnswers))
  }
}
