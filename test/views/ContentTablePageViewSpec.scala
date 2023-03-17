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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import utils.BaseSpec
import viewmodels.DisplayMessage.Message
import views.html.ContentTablePageView

class ContentTablePageViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  runningApplication { implicit app =>
    val view = injected[ContentTablePageView]

    implicit val request = FakeRequest()

    "ContentTablePageView" should {

      "render the title" in {

        forAll(contentTablePageViewModelGen) { viewModel =>
          title(view(viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the heading" in {

        forAll(contentTablePageViewModelGen) { viewModel =>
          h1(view(viewModel)) mustBe messageKey(viewModel.heading)
        }
      }

      "render the table rows" in {
        forAll(contentTablePageViewModelGen) { viewModel =>
          tr(view(viewModel)) must contain allElementsOf viewModel.rows.map {
            case (k, v) => List(messageKey(k), messageKey(v))
          }
        }
      }

      "render inset text" in {
        forAll(contentTablePageViewModelGen) { viewModel =>
          inset(view(viewModel)).text() mustBe viewModel.inset.asInstanceOf[Message].key
        }
      }

      "render the button text" in {

        forAll(contentTablePageViewModelGen) { viewModel =>
          anchorButton(view(viewModel)).content mustBe messageKey(viewModel.buttonText)
        }
      }

      "render the button href" in {

        forAll(contentTablePageViewModelGen) { viewModel =>
          anchorButton(view(viewModel)).href mustBe viewModel.onSubmit.url
        }
      }
    }
  }
}
