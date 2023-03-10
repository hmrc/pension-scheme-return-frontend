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
import views.html.ContentPageView


class ContentPageViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  runningApplication { implicit app =>
    val view = injected[ContentPageView]

    implicit val request = FakeRequest()

    "ContentPageView" should {

      "render the title" in {

        forAll(contentPageViewModelGen) { viewModel =>

          title(view(viewModel)) must startWith(viewModel.title.toMessage)
        }
      }

      "render the heading" in {

        forAll(contentPageViewModelGen) { viewModel =>

          h1(view(viewModel)) mustBe viewModel.heading.toMessage
        }
      }

      "render all paragraphs" in {

        forAll(contentPageViewModelGen) { viewModel =>

          p(view(viewModel)) must contain allElementsOf viewModel.paragraphs.map(_.toMessage)
        }
      }

      "render all listItems" in {

        forAll(contentPageViewModelGen) { viewModel =>

          li(view(viewModel)) must contain allElementsOf viewModel.listItems.map(_.toMessage)
        }
      }

      "render the button text" in {

        forAll(contentPageViewModelGen) { viewModel =>

          anchorButton(view(viewModel)).content mustBe viewModel.buttonText.key
        }
      }

      "render the button href" in {

        forAll(contentPageViewModelGen) { viewModel =>

          anchorButton(view(viewModel)).href mustBe viewModel.onSubmit.url
        }
      }
    }
  }
}