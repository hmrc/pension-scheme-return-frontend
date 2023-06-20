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

import models.{ConditionalYesNo, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.YesNoPageFormProvider
import views.html.{ConditionalYesNoPageView, YesNoPageView}
import IndividualRecipientNinoController._
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IndividualRecipientNinoPage}
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class IndividualRecipientNinoControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.IndividualRecipientNinoController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.IndividualRecipientNinoController.onSubmit(srn, NormalMode)

  val userAnswersWithIndividualName = defaultUserAnswers.unsafeSet(IndividualRecipientNamePage(srn), individualName)

  val conditionalNo: ConditionalYesNo[Nino] = ConditionalYesNo[Nino](Left("reason"))
  val conditionalYes: ConditionalYesNo[Nino] = ConditionalYesNo[Nino](Right(nino))

  "IndividualRecipientNinoController" - {

    act.like(renderView(onPageLoad, userAnswersWithIndividualName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, individualName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IndividualRecipientNinoPage(srn),
        conditionalNo,
        userAnswersWithIndividualName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, individualName, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> nino.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> nino.value))

    act.like(invalidForm(onSubmit, userAnswersWithIndividualName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
