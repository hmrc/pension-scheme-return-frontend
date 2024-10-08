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

package controllers.nonsipp.memberreceivedpcls

import utils.RefinedUtils.RefinedIntOps
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import views.html.MultipleQuestionView
import eu.timepit.refined.refineMV
import utils.Transform.TransformOps
import play.api.libs.json.JsPath
import forms.MoneyFormProvider
import models.{Money, NormalMode}
import models.PensionCommencementLumpSum._
import config.RefinedTypes.Max300
import controllers.ControllerBaseSpec

class PensionCommencementLumpSumAmountControllerSpec extends ControllerBaseSpec {

  val maxAllowedAmount = 999999999.99
  val index: Max300 = refineMV(1)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)

  "PensionCommencementLumpSumAmountController" - {

    val form = PensionCommencementLumpSumAmountController.form(new MoneyFormProvider())
    lazy val viewModel =
      PensionCommencementLumpSumAmountController.viewModel(srn, index, memberDetails.fullName, NormalMode, _)

    val lumpSumData = pensionCommencementLumpSumGen.sample.value

    lazy val onPageLoad = routes.PensionCommencementLumpSumAmountController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.PensionCommencementLumpSumAmountController.onSubmit(srn, index, NormalMode)

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(form, viewModel(form))
      }
    )

    act.like(
      renderPrePopView(
        onPageLoad,
        PensionCommencementLumpSumAmountPage(srn, index),
        lumpSumData,
        userAnswers
      ) { implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(form.fill(lumpSumData.from[(Money, Money)]), viewModel(form))
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        userAnswers,
        Some(
          JsPath \ "membersPayments" \ "memberDetails" \ "memberLumpSumReceived" \ index.arrayIndex.toString
        ),
        formData(form, lumpSumData.from[(Money, Money)]): _*
      )
    )

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(
      invalidForm(onSubmit, userAnswers, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )
  }
}
