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

package controllers.nonsipp

import play.api.test.FakeRequest
import controllers.ControllerBaseSpec
import config.Constants.{RETURN_PERIODS, SUBMISSION_DATE}
import play.api.libs.json.Json
import models.DateRange
import models.requests.psr.MinimalRequiredSubmission.nonEmptyListFormat
import viewmodels.models.SubmissionViewModel
import controllers.nonsipp.ReturnSubmittedController.viewModel
import cats.data.NonEmptyList
import views.html.SubmissionView

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReturnSubmittedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReturnSubmittedController.onPageLoad(srn).url

  private val returnPeriod1 = dateRangeGen.sample.value
  private val returnPeriod2 = dateRangeGen.sample.value
  private val returnPeriod3 = dateRangeGen.sample.value
  private val submissionDateTime = localDateTimeGen.sample.value

  private val pensionSchemeEnquiriesUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries"
  private val mpsDashboardUrl = "http://localhost:8204/manage-pension-schemes/pension-scheme-summary/" + srn.value

  "ReturnSubmittedController" - {

    "onPageLoads redirects when both required data not found in session" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onPageLoad)

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "onPageLoads redirects when SUBMISSION_DATE data not found in session" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onPageLoad).withSession((RETURN_PERIODS, "stub-return-periods"))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "onPageLoads redirects when RETURN_PERIODS data not found in session" in runningApplication { implicit app =>
      val request = FakeRequest(GET, onPageLoad).withSession((SUBMISSION_DATE, "stub-submission-date"))

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    List(
      ("tax year return period", NonEmptyList.one(returnPeriod1)),
      ("single accounting period", NonEmptyList.one(returnPeriod1)),
      ("multiple accounting periods", NonEmptyList.of(returnPeriod1, returnPeriod2, returnPeriod3))
    ).foreach {
      case (testName, returnPeriods) =>
        s"on arriving to page for the first time - $testName" in runningApplication { implicit app =>
          val view = injected[SubmissionView]
          val request = FakeRequest(GET, onPageLoad)
            .withSession((RETURN_PERIODS, Json.prettyPrint(Json.toJson(returnPeriods))))
            .withSession((SUBMISSION_DATE, submissionDateTime.format(DateTimeFormatter.ISO_DATE_TIME)))

          val result = route(app, request).value
          val expectedView = view(buildViewModel(returnPeriods, submissionDateTime))(
            request,
            createMessages(app)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView.toString
        }
    }

    act.like(journeyRecoveryPage(routes.ReturnSubmittedController.onPageLoad(srn)).updateName("onPageLoad" + _))
  }

  private def buildViewModel(
    returnPeriods: NonEmptyList[DateRange],
    submissionDate: LocalDateTime
  ): SubmissionViewModel =
    viewModel(
      schemeName,
      email,
      returnPeriods,
      submissionDate,
      pensionSchemeEnquiriesUrl,
      mpsDashboardUrl
    )
}
