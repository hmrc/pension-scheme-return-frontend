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

package views

import play.api.test.FakeRequest
import viewmodels.models.SummaryAction
import views.html.CheckYourAnswersView

class CheckYourAnswersViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[CheckYourAnswersView]

    implicit val request = FakeRequest()

    "CheckYourAnswerView" should {

      act.like(renderTitle(checkYourAnswersViewModelGen)(view(_), _.title.key))
      act.like(renderHeading(checkYourAnswersViewModelGen)(view(_), _.heading))
      act.like(renderSaveAndContinueButton(checkYourAnswersViewModelGen)(view(_)))

      "render the summary list keys" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>
          val keys = viewModel.rows.map(_.key.key)
          summaryListKeys(view(viewModel)) must contain theSameElementsAs keys
        }
      }

      "render the summary list values" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>
          val values = viewModel.rows.map(_.value.key)
          summaryListValues(view(viewModel)) must contain theSameElementsAs values
        }
      }

      "render the summary list actions" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>
          val actions =
            viewModel.rows
              .flatMap(_.actions)
              .map { case SummaryAction(content, href, vh) => AnchorTag(href, s"${content.key} ${vh.key}") }

          summaryListActions(view(viewModel)) must contain theSameElementsAs actions
        }
      }
    }
  }
}
