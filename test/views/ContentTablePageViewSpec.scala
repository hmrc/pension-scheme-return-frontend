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

import views.html.ContentTablePageView
import viewmodels.models.ContentTablePageViewModel

class ContentTablePageViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[ContentTablePageView]

    val viewModelGen = formPageViewModelGen[ContentTablePageViewModel]

    "ContentTablePageView" - {

      "render the title" in {

        forAll(viewModelGen) { viewModel =>
          title(view(viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the heading" in {

        forAll(viewModelGen) { viewModel =>
          h1(view(viewModel)) mustBe messageKey(viewModel.heading)
        }
      }

      "render the table rows" in {
        forAll(viewModelGen) { viewModel =>
          tr(view(viewModel)) must contain allElementsOf viewModel.page.rows.map { case (k, v) =>
            List(messageKey(k), messageKey(v))
          }
        }
      }

      "render inset text" in {
        forAll(viewModelGen) { viewModel =>
          inset(view(viewModel)).text() mustBe messageKey(viewModel.page.inset.value, " ")
        }
      }

      "render before text" in {
        forAll(viewModelGen) { viewModel =>
          p(view(viewModel)) must contain(messageKey(viewModel.page.beforeTable.value, " "))
        }
      }

      "render after text" in {
        forAll(viewModelGen) { viewModel =>
          p(view(viewModel)) must contain(messageKey(viewModel.page.afterTable.value, " "))
        }
      }

      "render the button text" in {

        forAll(viewModelGen) { viewModel =>
          anchorButton(view(viewModel)).content mustBe messageKey(viewModel.buttonText)
        }
      }

      "render the button href" in {

        forAll(viewModelGen) { viewModel =>
          anchorButton(view(viewModel)).href mustBe viewModel.onSubmit.url
        }
      }
    }
  }
}
