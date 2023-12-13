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

package controllers.nonsipp.memberreceivedpcls

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.memberreceivedpcls.PclsCYAController._
import eu.timepit.refined.refineMV
import models.{NormalMode, PensionSchemeType}
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import views.html.CheckYourAnswersView

class PclsCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private lazy val onPageLoad = routes.PclsCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.PclsCYAController.onSubmit(srn, index, NormalMode)

  private val pensionSchemeType = PensionSchemeType.Other("other")
  private val lumpSumAmounts = pensionCommencementLumpSumGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index, NormalMode), lumpSumAmounts)

  "PclsCYAController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, lumpSumAmounts, NormalMode)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
