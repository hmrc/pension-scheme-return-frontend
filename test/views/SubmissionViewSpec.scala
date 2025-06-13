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

package views

import forms.mappings.Mappings
import views.html.SubmissionView

class SubmissionViewSpec extends ViewSpec with ViewBehaviours with Mappings {
  runningApplication { implicit app =>
    val view = injected[SubmissionView]

    "SubmissionView" - {
      act.like(renderTitle(submissionViewModelGen)(view(_), _.title.key))

      "render panel" in {
        forAll(submissionViewModelGen) { viewModel =>
          val panelElement = panel(view(viewModel))
          panelElement.title.text() mustBe viewModel.panelHeading.key
          panelElement.body.map(_.text()) mustBe Some(viewModel.panelContent.key)
        }
      }

      "render the summary list keys" in {

        forAll(submissionViewModelGen) { viewModel =>
          val scheme = messages("returnSubmitted.table.field1")
          val periodOfReturn = messages("returnSubmitted.table.field2")
          val dateSubmitted = messages("returnSubmitted.table.field3")
          summaryListKeys(view(viewModel)) must contain theSameElementsAs List(scheme, periodOfReturn, dateSubmitted)
        }
      }

      "render the summary list values" in {
        forAll(submissionViewModelGen) { viewModel =>
          val dateSubmitted = messageKey(viewModel.dateSubmitted)
          val periodOfReturn = messageKey(viewModel.periodOfReturn)
          val scheme = messageKey(viewModel.scheme)

          summaryListValues(view(viewModel)) must contain theSameElementsAs List(scheme, periodOfReturn, dateSubmitted)
        }
      }

      "render the email" in {
        forAll(submissionViewModelGen) { viewModel =>
          val email = viewModel.email.map(messageKey(_)).getOrElse("")
          mainContent(view(viewModel)).getElementById("content").text() must include(email)
        }
      }

      "render h2" in {
        forAll(submissionViewModelGen) { viewModel =>
          h2(view(viewModel)) must contain(messages("site.whatHappensNext"))
        }
      }

      "render what happens next content" in {
        forAll(submissionViewModelGen) { viewModel =>
          mainContent(view(viewModel)).getElementById("what-happens-next-content").text() mustBe messageKey(
            viewModel.whatHappensNextContent
          )
        }
      }
    }
  }
}
