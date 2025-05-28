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

package controllers.nonsipp.memberdetails

import play.api.test.FakeRequest
import controllers.ControllerBaseSpec
import views.html.NameDOBView
import eu.timepit.refined.refineMV
import forms.NameDOBFormProvider
import models.{NameDOB, NormalMode}
import play.api.i18n.Messages
import play.api.data.FormError
import play.api.test.Helpers.stubMessagesApi
import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.nonsipp.memberdetails.MemberDetailsController._

import java.time.LocalDate

class MemberDetailsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.MemberDetailsController.onPageLoad(srn, 1, NormalMode)
  private lazy val onSubmit = routes.MemberDetailsController.onSubmit(srn, 1, NormalMode)

  private val validForm = List(
    "firstName" -> "testFirstName",
    "lastName" -> "testLastName",
    "dateOfBirth.day" -> "12",
    "dateOfBirth.month" -> "12",
    "dateOfBirth.year" -> "2020"
  )

  private val dobInFutureForm = List(
    "firstName" -> "testFirstName",
    "lastName" -> "testLastName",
    "dateOfBirth.day" -> "12",
    "dateOfBirth.month" -> "12",
    "dateOfBirth.year" -> (LocalDate.now().getYear + 1).toString
  )

  private val dobTooEarlyForm = List(
    "firstName" -> "testFirstName",
    "lastName" -> "testLastName",
    "dateOfBirth.day" -> "31",
    "dateOfBirth.month" -> "12",
    "dateOfBirth.year" -> "1899"
  )

  private val nameDOB = NameDOB(
    "testFirstName",
    "testLastName",
    LocalDate.of(2020, 12, 12)
  )

  "MemberDetailsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[NameDOBView]
        .apply(form(injected[NameDOBFormProvider], None), viewModel(srn, refineMV(1), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, MemberDetailsPage(srn, refineMV(1)), nameDOB) {
      implicit app => implicit request =>
        val preparedForm = form(injected[NameDOBFormProvider], None).fill(nameDOB)
        injected[NameDOBView].apply(preparedForm, viewModel(srn, refineMV(1), NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, validForm: _*))
    act.like(invalidForm(onSubmit))
    act.like(invalidForm(onSubmit, dobInFutureForm: _*))
    act.like(invalidForm(onSubmit, dobTooEarlyForm: _*))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "must bind validation errors when earlier than earliest date is submitted" in {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val stubMessages: Messages = stubMessagesApi().preferred(FakeRequest())

    running(application) {
      val formProvider = application.injector.instanceOf[NameDOBFormProvider]
      val testForm = MemberDetailsController.form(formProvider, Some(LocalDate.now()))
      val boundForm = testForm.bind(
        Map(
          "dateOfBirth.day" -> tooEarlyDate.getDayOfMonth.toString,
          "dateOfBirth.month" -> tooEarlyDate.getMonthValue.toString,
          "dateOfBirth.year" -> tooEarlyDate.getYear.toString
        )
      )
      boundForm.errors must contain(FormError("dateOfBirth", "memberDetails.dateOfBirth.error.after"))
    }
  }
}
