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

import forms.YesNoPageFormProvider
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import utils.BaseSpec
import views.html.YesNoPageView

class YesNoPageViewSpec extends ViewSpec {

  runningApplication { implicit app =>

    implicit val request  = FakeRequest()

    val view = injected[YesNoPageView]

    "YesNoPageView" should {

      val requiredKey = "required"
      val invalidKey = "invalid"

      val yesNoForm = new YesNoPageFormProvider()(requiredKey, invalidKey)

      behave like renderTitle(yesNoPageViewModelGen)(view(yesNoForm, _), _.title.key)
      behave like renderHeading(yesNoPageViewModelGen)(view(yesNoForm, _), _.heading)
      behave like renderSaveAndContinueButton(yesNoPageViewModelGen)(view(yesNoForm, _))
      behave like renderForm(yesNoPageViewModelGen)(view(yesNoForm, _), _.onSubmit)

      "have a description when present" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          p(view(yesNoForm, viewmodel)) must contain allElementsOf viewmodel.description.map(_.toMessage)
        }
      }

      "does not have a description when not present" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          p(view(yesNoForm, viewmodel.copy(description = Nil))) mustBe Nil
        }
      }

      "have a legend" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          whenever(viewmodel.legend.nonEmpty) {

            legend(view(yesNoForm, viewmodel)) must contain(viewmodel.legend.map(_.toMessage).value)
          }
        }
      }

      "have radio button values" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          radios(view(yesNoForm, viewmodel)).map(_.value) mustBe List("true", "false")
        }
      }

      "have radio button labels" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          radios(view(yesNoForm, viewmodel)).map(_.label) mustBe List(messages("site.yes"), messages("site.no"))
        }
      }

      "have error summary" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          val invalidForm = yesNoForm.bind(Map("value" -> ""))
          errorSummary(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }

      "have error message" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          val invalidForm = yesNoForm.bind(Map("value" -> ""))
          errorMessage(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }
    }
  }
}