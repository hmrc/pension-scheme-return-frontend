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

package controllers.nonsipp.common

import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import models.IdentityType._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{IdentitySubject, NormalMode}
import controllers.nonsipp.common.IdentityTypeController._
import models.requests.AllowedAccessRequest

class IdentityTypeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value

  "IdentityTypeController" - {
    IdentitySubject.values.foreach { identitySubject =>
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, index, NormalMode, identitySubject)
      lazy val onSubmit =
        controllers.nonsipp.common.routes.IdentityTypeController
          .onSubmit(srn, index, NormalMode, identitySubject)

      act.like(renderView(onPageLoad) { implicit app => implicit request =>
        val view = injected[RadioListView]
        view(
          form(injected[RadioListFormProvider], identitySubject),
          viewModel(srn, index, NormalMode, identitySubject, defaultUserAnswers)
        )
      }.withName(s"identity type page rendered ok for $identitySubject"))

      act.like(
        journeyRecoveryPage(onPageLoad)
          .updateName("onPageLoad " + _)
          .withName(s"identity type page journeyRecoveryPage ok for $identitySubject")
      )

      act.like(
        saveAndContinue(onSubmit, "value" -> Individual.name)
          .withName(s"an Individual data is submitted for $identitySubject")
      )

      act.like(
        saveAndContinue(onSubmit, "value" -> UKCompany.name)
          .withName(s"a UKCompany data is submitted for $identitySubject")
      )

      act.like(
        saveAndContinue(onSubmit, "value" -> UKPartnership.name)
          .withName(s"aUKPartnership data is submitted for $identitySubject")
      )

      act.like(
        saveAndContinue(onSubmit, "value" -> Other.name)
          .withName(s"other data is submitted for $identitySubject")
      )

      act.like(
        journeyRecoveryPage(onSubmit)
          .updateName("onSubmit" + _)
          .withName(s"journey recovery on submit for $identitySubject")
      )
    }

    "Unknown" - {
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.Unknown)

      act.like(
        unauthorisedPage(onPageLoad, Some(defaultUserAnswers))
          .updateName("onPageLoad " + _)
      )
    }
  }
}
