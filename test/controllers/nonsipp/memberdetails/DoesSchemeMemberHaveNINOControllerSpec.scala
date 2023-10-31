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

package controllers.nonsipp.memberdetails

import DoesSchemeMemberHaveNINOController._
import config.Refined.Max300
import controllers.ControllerBaseSpec
import controllers.nonsipp.memberdetails.routes
import eu.timepit.refined.refineMV
import forms.{TextFormProvider, YesNoPageFormProvider}
import models.{ConditionalYesNo, NormalMode}
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsPage}
import uk.gov.hmrc.domain.Nino
import views.html.{ConditionalYesNoPageView, TextAreaView, YesNoPageView}

class DoesSchemeMemberHaveNINOControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)

  private lazy val onPageLoad = routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.DoesSchemeMemberHaveNINOController.onSubmit(srn, refineMV(1), NormalMode)

  private val userAnswersWithMemberDetails =
    defaultUserAnswers.set(MemberDetailsPage(srn, refineMV(1)), memberDetails).success.value

  val conditionalNo: ConditionalYesNo[String, Nino] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Nino] = ConditionalYesNo.yes(nino)

  private val memberName: String = "memberName"

  "NationalInsuranceNumberController" - {

    act.like(renderView(onPageLoad, userAnswersWithMemberDetails) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(
        form(injected[YesNoPageFormProvider], memberDetails.fullName, List()),
        viewModel(srn, index, memberDetails.fullName, NormalMode)
      )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        DoesMemberHaveNinoPage(srn, index),
        conditionalNo,
        userAnswersWithMemberDetails
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], memberName, List()).fill(conditionalNo.value),
            viewModel(srn, index, memberDetails.fullName, NormalMode)
          )
      }
    )

//    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> nino.value))
//    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswersWithMemberDetails))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
