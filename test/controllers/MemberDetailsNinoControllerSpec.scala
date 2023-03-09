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

package controllers

import eu.timepit.refined.refineMV
import forms.{DateRangeFormProvider, TextFormProvider}
import models.{DateRange, NormalMode}
import pages.{AccountingPeriodPage, MemberDetailsNinoPage, MemberDetailsPage}
import views.html.{DateRangeView, TextInputView}

class MemberDetailsNinoControllerSpec extends ControllerBaseSpec {

  "MemberDetailsNinoController" should {

    val memberDetails = nameDobGen.sample.value
    val populatedUserAnswers = defaultUserAnswers.set(MemberDetailsPage(srn, refineMV(1)), memberDetails).get

    val form = MemberDetailsNinoController.form(new TextFormProvider(), memberDetails, List())
    lazy val viewModel = MemberDetailsNinoController.viewModel(srn, refineMV(1), NormalMode, memberDetails)

    val validNino = ninoGen.sample.value
    val otherValidNino = ninoGen.sample.value

    lazy val onPageLoad = routes.MemberDetailsNinoController.onPageLoad(srn, refineMV(1), NormalMode)
    lazy val onSubmit = routes.MemberDetailsNinoController.onSubmit(srn, refineMV(1), NormalMode)

    behave like renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      val view = injected[TextInputView]
      view(form, viewModel)
    }

    behave like renderPrePopView(onPageLoad, MemberDetailsNinoPage(srn, refineMV(1)), validNino, populatedUserAnswers) {
      implicit app => implicit request =>
        val view = injected[TextInputView]
        view(form.fill(validNino), viewModel)
    }

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    "when no member details exists for onPageLoad" when {

      behave like redirectWhenCacheEmpty(onPageLoad, routes.JourneyRecoveryController.onPageLoad())
    }

    "when no member details exists for onSubmit" when {

      behave like redirectWhenCacheEmpty(onSubmit, routes.JourneyRecoveryController.onPageLoad())
    }

    behave like saveAndContinue(onSubmit, populatedUserAnswers, formData(form, validNino): _*)
    
    behave like invalidForm(onSubmit, populatedUserAnswers)

    behave like journeyRecoveryPage("onSubmit", onSubmit)

    "allow nino to be updated" when {
      val userAnswers = populatedUserAnswers.set(MemberDetailsNinoPage(srn, refineMV(1)), validNino).get
      behave like saveAndContinue(onSubmit, userAnswers, formData(form, validNino): _*)
    }

    "return a 400 if nino has already been entered" when {
      val userAnswers =
        populatedUserAnswers
          .set(MemberDetailsNinoPage(srn, refineMV(1)), otherValidNino).get
          .set(MemberDetailsNinoPage(srn, refineMV(2)), validNino).get

      behave like invalidForm(onSubmit, userAnswers, formData(form, validNino): _*)
    }
  }
}