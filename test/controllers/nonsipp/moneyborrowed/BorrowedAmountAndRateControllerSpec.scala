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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MultipleQuestionView
import utils.IntUtils.given
import models.NormalMode
import pages.nonsipp.moneyborrowed.BorrowedAmountAndRatePage

class BorrowedAmountAndRateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  val maxAllowedAmount = 999999999.99
  private val index = 1

  "BorrowedAmountAndRateController" - {

    val schemeName = defaultSchemeDetails.schemeName

    lazy val viewModel = BorrowedAmountAndRateController.viewModel(srn, index, NormalMode, schemeName, _)

    lazy val onPageLoad = routes.BorrowedAmountAndRateController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.BorrowedAmountAndRateController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(
        BorrowedAmountAndRateController.form(),
        viewModel(BorrowedAmountAndRateController.form())
      )
    })

    act.like(renderPrePopView(onPageLoad, BorrowedAmountAndRatePage(srn, index), (money, percentage)) {
      implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(
          BorrowedAmountAndRateController.form().fill((money, percentage)),
          viewModel(BorrowedAmountAndRateController.form())
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString
      )
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
