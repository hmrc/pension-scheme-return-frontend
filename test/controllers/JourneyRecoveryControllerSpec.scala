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

package controllers

import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import config.FrontendAppConfig
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import config.Constants.SRN

class JourneyRecoveryControllerSpec extends ControllerBaseSpec with ControllerBehaviours with TestValues {
  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  override def beforeEach(): Unit =
    super.beforeEach()

  "JourneyRecovery Controller" - {

    "return OK and the continue view" - {
      "a relative continue Url is supplied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val expectedReportAProblemUrl = appConfig.reportAProblemUrl

          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

          status(result) mustEqual OK
          val expectedView = continueView(continueUrl.unsafeValue, expectedReportAProblemUrl)(
            request,
            createMessages(application)
          ).toString
          val actualView = contentAsString(result)
          actualView mustEqual expectedView
        }
      }
    }

    "return OK and the start again view" - {
      "an absolute continue Url is supplied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val expectedReportAProblemUrl = appConfig.reportAProblemUrl
          val expectedRedirectUrl = appConfig.urls.managePensionsSchemes.overview
          val continueUrl = RedirectUrl("https://foo.com")
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView(expectedRedirectUrl, expectedReportAProblemUrl)(
            request,
            createMessages(application)
          ).toString
        }
      }
    }

    "return OK and the start again view" - {
      "no continue Url is supplied" - {
        "and srn is not in the session" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val appConfig = application.injector.instanceOf[FrontendAppConfig]
            val expectedReportAProblemUrl = appConfig.reportAProblemUrl

            val expectedRedirectUrl = appConfig.urls.managePensionsSchemes.overview

            val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

            val result = route(application, request).value

            val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual startAgainView(expectedRedirectUrl, expectedReportAProblemUrl)(
              request,
              createMessages(application)
            ).toString
          }
        }
        "and srn is in the session" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val appConfig = application.injector.instanceOf[FrontendAppConfig]
            val expectedReportAProblemUrl = appConfig.reportAProblemUrl
            val expectedRedirectUrl = controllers.nonsipp.routes.TaskListController.onPageLoad(srn).url

            val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)
              .withSession((SRN, srn.value))

            val result = route(application, request).value

            val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual startAgainView(expectedRedirectUrl, expectedReportAProblemUrl)(
              request,
              createMessages(application)
            ).toString
          }
        }
      }
    }
  }
}
