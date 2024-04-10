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

import play.api.test.FakeRequest
import views.html.CheckYourAnswersView
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersViewModel, SummaryAction}

class CheckYourAnswersViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[CheckYourAnswersView]

    implicit val request = FakeRequest()

    val viewModelGen = formPageViewModelGen[CheckYourAnswersViewModel]

    "CheckYourAnswerView" - {

      act.like(renderTitle(viewModelGen)(view(_), _.title.key))
      act.like(renderHeading(viewModelGen)(view(_), _.heading))
      act.like(renderContinueButtonWithText(viewModelGen)(view(_), _.buttonText.key))

      "render the summary list keys" in {

        forAll(viewModelGen) { viewModel =>
          val keys = viewModel.page.sections.flatMap(_.rows.map(_.key.key))
          summaryListKeys(view(viewModel)) must contain theSameElementsAs keys
        }
      }

      "render the summary list values" in {

        forAll(viewModelGen) { viewModel =>
          val values = viewModel.page.sections.flatMap(_.rows.collect(_.value match {
            case m: Message => m.key
          }))
          summaryListValues(view(viewModel)) must contain theSameElementsAs values
        }
      }

      "render the summary list actions" in {

        forAll(viewModelGen) { viewModel =>
          val actions =
            viewModel.page.sections.flatMap(
              _.rows
                .flatMap(_.actions)
                .map { case SummaryAction(content, href, vh) => AnchorTag(href, s"${content.key} ${vh.key}") }
            )

          summaryListActions(view(viewModel)) must contain theSameElementsAs actions
        }
      }
    }
  }
}
