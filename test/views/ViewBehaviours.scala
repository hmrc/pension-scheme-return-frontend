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

import org.scalacheck.Gen
import play.twirl.api.Html
import viewmodels.DisplayMessage

trait ViewBehaviours {
  _: ViewSpec =>

  def renderTitle[A](gen: Gen[A])(view: A => Html, key: A => String): Unit =
    "render the title" in {
      forAll(gen) { viewModel =>
        title(view(viewModel)) must startWith(key(viewModel))
      }
    }

  def renderHeading[A](gen: Gen[A])(view: A => Html, key: A => String): Unit =
    "render the heading" in {
      forAll(gen) { viewModel =>
        h1(view(viewModel)) must startWith(key(viewModel))
      }
    }

  def renderButtonText[A](gen: Gen[A])(view: A => Html, key: A => DisplayMessage): Unit =
    "render the button text" in {
      forAll(gen) { viewModel =>
        button(view(viewModel)).text() mustBe messageKey(key(viewModel))
      }
    }
}
