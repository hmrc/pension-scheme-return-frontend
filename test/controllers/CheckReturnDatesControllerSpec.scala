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
import services.{SaveService, SchemeDetailsService}
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import scala.concurrent.Future

class CheckReturnDatesControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks { self =>

  def onwardRoute = Call("GET", "/foo")

  lazy val checkReturnDatesRoute = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url

  "CheckReturnDates.viewModel" should {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value

    "contain correct title key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.title mustBe SimpleMessage("checkReturnDates.title")
      }
    }

    "contain correct heading key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.heading mustBe SimpleMessage("checkReturnDates.heading")
      }
    }

    "contain from date when it is after open date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = Some(earliestDate), windUpDate = None)

      forAll(date, date) { (fromDate, toDate) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(SimpleMessage("checkReturnDates.description", formattedFromDate, formattedToDate))
      }
    }

    "contain open date when it is after from date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(windUpDate = None))

      forAll(detailsGen, date) { (details, toDate) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, earliestDate, toDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(details.openDate.getOrElse(earliestDate))
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(SimpleMessage("checkReturnDates.description", formattedFromDate, formattedToDate))
      }
    }

    "contain to date when it is before wind up date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = None, windUpDate = Some(latestDate))

      forAll(date, date) { (fromDate, toDate) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(SimpleMessage("checkReturnDates.description", formattedFromDate, formattedToDate))
      }
    }

    "contain wind up date when it is before to date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(openDate = None))

      forAll(detailsGen, date) { (details, fromDate) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, latestDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(details.windUpDate.getOrElse(latestDate))

        viewModel.description mustBe List(SimpleMessage("checkReturnDates.description", formattedFromDate, formattedToDate))
      }
    }

    "contain correct legend key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.legend.value mustBe SimpleMessage("checkReturnDates.legend")
      }
    }

    "populate the onSubmit with srn and mode" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>

        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.onSubmit mustBe routes.CheckReturnDatesController.onSubmit(srn, mode)
      }
    }
  }


  "CheckReturnDates Controller" should {

    val date = self.date.sample.value
    val taxYear = TaxYear(date.getYear)
    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    lazy val viewModel: YesNoPageViewModel =
      CheckReturnDatesController.viewModel(
        srn,
        NormalMode,
        taxYear.starts,
        taxYear.finishes,
        minimalSchemeDetails
      )

    val formProvider = new YesNoPageFormProvider()
    val form = formProvider("checkReturnDates.error.required", "checkReturnDates.error.invalid")

    val mockSchemeDetailsService = mock[SchemeDetailsService]
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(minimalSchemeDetails)))

    def applicationBuilder(userAnswers: Option[UserAnswers]) =
      self.applicationBuilder(userAnswers, taxYear = taxYear)
        .overrides(
          bind[SchemeDetailsService].toInstance(mockSchemeDetailsService)
        )

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

      when(mockSaveService.save(any())(any(), any())) thenReturn Future.successful(())

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
