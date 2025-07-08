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
import views.html.PsrLockedView
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class PsrLockedControllerSpec extends ControllerBaseSpec with ControllerBehaviours with TestValues {
  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  override def beforeEach(): Unit =
    super.beforeEach()

  "Psr locked controller" - {

    "return OK and Psl locked view" - {
      "todo" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val expectedReportAProblemUrl = appConfig.reportAProblemUrl

          val request = FakeRequest(GET, routes.PsrLockedController.onPageLoad(srn).url)

          val result = route(application, request).value

          val lockedView = application.injector.instanceOf[PsrLockedView]

          status(result) mustEqual OK
          val expectedView =
            lockedView(controllers.routes.OverviewController.onPageLoad(srn).url, expectedReportAProblemUrl)(using
              request,
              createMessages(using application)
            ).toString
          val actualView = contentAsString(result)
          actualView mustEqual expectedView
        }
      }
    }
  }
}
