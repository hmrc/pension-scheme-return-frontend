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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ConditionalYesNoPageView
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.common.PartnershipRecipientUtrPage
import utils.IntUtils.given
import controllers.nonsipp.common.PartnershipRecipientUtrController._

class PartnershipRecipientUtrControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "PartnershipRecipientUtrController" - {
    IdentitySubject.values.foreach { identitySubject =>
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.PartnershipRecipientUtrController
          .onPageLoad(srn, index, NormalMode, identitySubject)
      lazy val onSubmit =
        controllers.nonsipp.common.routes.PartnershipRecipientUtrController
          .onSubmit(srn, index, NormalMode, identitySubject)

      act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], identitySubject),
            viewModel(srn, index, NormalMode, identitySubject, defaultUserAnswers)
          )
      }.withName(s"should render PartnershipRecipientUtrController for $identitySubject"))

      act.like(
        renderPrePopView(
          onPageLoad,
          PartnershipRecipientUtrPage(srn, index, identitySubject),
          conditionalNo,
          defaultUserAnswers
        ) { implicit app => implicit request =>
          injected[ConditionalYesNoPageView]
            .apply(
              form(injected[YesNoPageFormProvider], identitySubject).fill(conditionalNo.value),
              viewModel(srn, index, NormalMode, identitySubject, defaultUserAnswers)
            )
        }.withName(s"should render PartnershipRecipientUtrController with pre populated view for $identitySubject")
      )

      act.like(
        redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value)
          .withName(s"should redirect to next page when value is true for $identitySubject")
      )
      act.like(
        redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason")
          .withName(s"should redirect to next page when value is false for $identitySubject")
      )

      act.like(
        journeyRecoveryPage(onPageLoad)
          .updateName("onPageLoad" + _)
          .withName(s"should redirect to Journey Recovery if no existing data is found, for $identitySubject")
      )

      act.like(
        saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value)
          .withName(s"should save data and continue to next page with for $identitySubject")
      )

      act.like(
        invalidForm(onSubmit, defaultUserAnswers)
          .withName(s"should return BAD_REQUEST for a POST with invalid form for $identitySubject")
      )
      act.like(
        journeyRecoveryPage(onSubmit)
          .updateName("onSubmit" + _)
          .withName(
            s"should redirect to Journey Recovery if no existing data is found when submitting, for $identitySubject"
          )
      )
    }

    "Unknown" - {
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.PartnershipRecipientUtrController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.Unknown)

      act.like(
        unauthorisedPage(onPageLoad, Some(defaultUserAnswers))
          .updateName("onPageLoad " + _)
      )
    }

  }
}
