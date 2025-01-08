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

import controllers.nonsipp.shares.SharesCheckAndUpdateController._
import pages.nonsipp.shares.{ClassOfSharesPage, CostOfSharesPage, TypeOfSharesHeldPage}
import views.html.ContentTablePageView
import eu.timepit.refined.refineMV
import models.NormalMode
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class SharesCheckAndUpdateControllerSpec extends ControllerBaseSpec {

  private val index: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)

  private def onPageLoad = routes.SharesCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = routes.SharesCheckAndUpdateController.onSubmit(srn, index)

  private val typeOfShares = typeOfSharesGen.sample.value

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(TypeOfSharesHeldPage(srn, index), typeOfShares)
    .unsafeSet(ClassOfSharesPage(srn, index), classOfShares)
    .unsafeSet(CostOfSharesPage(srn, index), money)

  "SharesCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[ContentTablePageView].apply(
          viewModel(
            srn = srn,
            index = index,
            typeOfShares,
            classOfShares,
            money
          )
        )
      }.withName(s"render correct view")
    )

    act.like(
      redirectToPage(onSubmit, routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode))
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .updateName("onSubmit" + _)
    )
  }
}
