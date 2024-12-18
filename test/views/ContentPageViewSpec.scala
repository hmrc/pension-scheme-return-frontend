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

import views.html.ContentPageView
import viewmodels.models.ContentPageViewModel

class ContentPageViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[ContentPageView]

    val viewModelGen = formPageViewModelGen[ContentPageViewModel]

    "ContentPageView" - {

      act.like(renderTitle(viewModelGen)(view(_), _.title.key))
      act.like(renderHeading(viewModelGen)(view(_), _.heading))
      act.like(renderDescription(viewModelGen)(view(_), _.description))

      "render the button text" in {

        forAll(viewModelGen) { viewModel =>
          anchorButton(view(viewModel)).content mustBe viewModel.buttonText.key
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
