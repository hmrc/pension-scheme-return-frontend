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

import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.CheckReturnDatesPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{FakeTaxYearService, SaveService, TaxYearService}
import utils.DateTimeUtils
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import scala.concurrent.Future

class CheckReturnDatesControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks { self =>

  def onwardRoute = Call("GET", "/foo")

  val srn = srnGen.sample.value
  lazy val checkReturnDatesRoute = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url

  "CheckReturnDates.viewModel" should {

    "contain correct title key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.title mustBe SimpleMessage("checkReturnDates.title")
      }
    }

    "contain correct heading key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.heading mustBe SimpleMessage("checkReturnDates.heading")
      }
    }

    "contain from date and to date in description" in {

      forAll(date, date) { (fromDate, toDate) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(SimpleMessage("checkReturnDates.description", formattedFromDate, formattedToDate))
      }
    }

    "contain correct legend key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.legend mustBe SimpleMessage("checkReturnDates.legend")
      }
    }

    "populate the onSubmit with srn and mode" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.onSubmit mustBe routes.CheckReturnDatesController.onSubmit(srn, mode)
      }
    }
  }


  "CheckReturnDates Controller" should {

    val date = self.date.sample.value
    val fakeTaxYearService = new FakeTaxYearService(date)
    lazy val viewModel: YesNoPageViewModel =
      CheckReturnDatesController.viewModel(
        srn,
        NormalMode,
        fakeTaxYearService.current.starts,
        fakeTaxYearService.current.finishes
      )

    val formProvider = new YesNoPageFormProvider()
    val form = formProvider("checkReturnDates.error.required", "checkReturnDates.error.invalid")

    def applicationBuilder(userAnswers: Option[UserAnswers]) =
      self.applicationBuilder(userAnswers).overrides(bind[TaxYearService].toInstance(fakeTaxYearService))

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkReturnDatesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YesNoPageView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel)(request, createMessages(application)).body
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(CheckReturnDatesPage(srn), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkReturnDatesRoute)

        val view = application.injector.instanceOf[YesNoPageView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), viewModel)(request, createMessages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSaveService = mock[SaveService]

      when(mockSaveService.save(any())(any())) thenReturn Future.successful(())

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SaveService].toInstance(mockSaveService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, checkReturnDatesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkReturnDatesRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[YesNoPageView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModel)(request, createMessages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkReturnDatesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, checkReturnDatesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
