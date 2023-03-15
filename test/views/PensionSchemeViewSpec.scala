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

import forms.PensionSchemeForm
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import utils.BaseSpec
import views.html.PensionSchemeView

class PensionSchemeViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  runningApplication { implicit app =>
    val view = injected[PensionSchemeView]

    implicit val request = FakeRequest()

    "PensionsSchemeView" should {

      val pensionSchemeForm = new PensionSchemeForm().apply(requiredKey = "required")

      "render the title" in {

        forAll(pensionSchemeViewModelGen) { viewModel =>
          title(view(pensionSchemeForm, viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the header" in {

        forAll(pensionSchemeViewModelGen) { viewModel =>
          h1(view(pensionSchemeForm, viewModel)) mustBe viewModel.heading.key
        }
      }

      "render the form" in {

        forAll(pensionSchemeViewModelGen) { viewModel =>
          form(view(pensionSchemeForm, viewModel)).method mustBe viewModel.onSubmit.method
          form(view(pensionSchemeForm, viewModel)).action mustBe viewModel.onSubmit.url
        }
      }

      "render the correct error summary" in {

        forAll(pensionSchemeViewModelGen) { viewModel =>
          val errorForm = pensionSchemeForm.bind(Map("value" -> ""))
          errorSummary(view(errorForm, viewModel)).text() must include("required")
        }
      }

      "render the correct error message" in {

        forAll(pensionSchemeViewModelGen) { viewModel =>
          val errorForm = pensionSchemeForm.bind(Map("value" -> ""))
          errorMessage(view(errorForm, viewModel)).text() must include("required")
        }
      }
    }
  }
}
