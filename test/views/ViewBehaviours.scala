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

import play.api.mvc.Call
import play.twirl.api.Html
import org.scalacheck.Gen
import viewmodels.DisplayMessage

trait ViewBehaviours {
  _: ViewSpec =>

  def renderTitle[A](gen: Gen[A])(view: A => Html, key: A => String): Unit =
    "render the title" in {
      forAll(gen) { viewModel =>
        title(view(viewModel)) must startWith(key(viewModel))
      }
    }

  def renderHeading[A](gen: Gen[A])(view: A => Html, key: A => DisplayMessage): Unit =
    "render the heading" in {
      forAll(gen) { viewModel =>
        h1(view(viewModel)) must startWith(messageKey(key(viewModel)))
      }
    }

  def renderInputWithLabel[A](gen: Gen[A])(name: String, view: A => Html, key: A => DisplayMessage): Unit =
    s"render the input for $name with label" in {

      forAll(gen) { viewModel =>
        val foundInput = input(view(viewModel))(name)

        assert(foundInput.nonEmpty)
        foundInput.map(_.label must startWith(messageKey(key(viewModel))))
      }
    }

  def renderDescription[A](gen: Gen[A])(view: A => Html, key: A => Option[DisplayMessage]): Behaviours.BehaviourTest =
    Behaviours.BehaviourTest(
      "render the description",
      () => {
        forAll(gen) { viewmodel =>
          elementText(view(viewmodel)).flatMap(_.split(" ")) must contain allElementsOf
            key(viewmodel).toList.flatMap(allMessages).map(_.key)
        }
      }
    )

  def renderInputWithH1Label[A](
    gen: Gen[A]
  )(name: String, view: A => Html, heading: A => DisplayMessage, label: A => Option[DisplayMessage]): Unit =
    s"render the input for $name with label" in {
      forAll(gen) { viewModel =>
        label(viewModel) match {
          case None =>
            inputLabel(view(viewModel))(name).text() mustBe messageKey(heading(viewModel))
            input(view(viewModel))(name).isEmpty mustEqual false
          case Some(label) =>
            inputLabel(view(viewModel))(name).text() mustBe messageKey(label)
            input(view(viewModel))(name).isEmpty mustEqual false
        }
      }
    }

  def renderButtonText[A](gen: Gen[A])(view: A => Html, key: A => DisplayMessage): Unit =
    "render the button text" in {
      forAll(gen) { viewModel =>
        button(view(viewModel)).text() mustBe messageKey(key(viewModel))
      }
    }

  def renderContinueButtonWithText[A](gen: Gen[A])(view: A => Html, expectedText: A => String): Unit =
    "render the button text" in {
      forAll(gen) { viewModel =>
        buttons(view(viewModel)).last().text() mustBe expectedText(viewModel)
      }
    }

  def renderSaveAndContinueButton[A](gen: Gen[A])(view: A => Html): Unit =
    "render the button text" in {
      forAll(gen) { viewModel =>
        buttons(view(viewModel)).last().text() mustBe "Save and continue"
      }
    }

  def renderForm[A](gen: Gen[A])(view: A => Html, call: A => Call): Unit =
    "has form" in {
      forAll(gen) { viewModel =>
        form(view(viewModel)).method mustBe call(viewModel).method
        form(view(viewModel)).action mustBe call(viewModel).url
      }
    }

  def renderUpload[A](gen: Gen[A])(view: A => Html): Unit =
    "render upload" in {
      forAll(gen) { viewmodel =>
        upload(view(viewmodel)).text() mustBe ""
      }
    }
  def renderDateInput[A](gen: Gen[A])(name: String, view: A => Html): Unit =
    s"render date input for $name" in {
      forAll(gen) { viewModel =>
        val elements = date(view(viewModel))(name)
        elements.day.text() mustEqual "Day"
        elements.month.text() mustEqual "Month"
        elements.year.text() mustEqual "Year"
      }
    }

  def renderTextArea[A](gen: Gen[A])(view: A => Html, name: String): Unit =
    s"render text area for $name" in {
      forAll(gen) { viewModel =>
        textAreas(view(viewModel))(name).size() mustBe 1
      }
    }

  def renderInset[A](gen: Gen[A])(view: A => Html, text: A => String): Behaviours.BehaviourTest =
    "render inset text".hasBehaviour {
      forAll(gen) { viewModel =>
        inset(view(viewModel)).text() must include(text(viewModel))
      }
    }

  def renderDetails[A](gen: Gen[A])(view: A => Html, text: A => String): Behaviours.BehaviourTest =
    "render details".hasBehaviour {
      forAll(gen) { viewModel =>
        details(view(viewModel)).text() must include(text(viewModel))
      }
    }

  def renderErrors[A](gen: Gen[A])(view: A => Html, error: A => String): Behaviours.MultipleBehaviourTests =
    Behaviours.MultipleBehaviourTests(
      "render all errors",
      List(
        "render required error summary".hasBehaviour {
          forAll(gen) { viewModel =>
            errorSummary(view(viewModel)).text() must include(error(viewModel))
          }
        },
        "render required error message".hasBehaviour {
          forAll(gen) { viewModel =>
            errorMessage(view(viewModel)).text() must include(error(viewModel))
          }
        }
      )
    )
}
