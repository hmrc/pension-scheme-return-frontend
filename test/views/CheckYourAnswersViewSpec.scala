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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList}
import utils.BaseSpec
import views.html.CheckYourAnswersView

import scala.language.postfixOps

class CheckYourAnswersViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  runningApplication { implicit app =>

    val view = injected[CheckYourAnswersView]

    implicit val request = FakeRequest()
    implicit val mess = messages(app)

    "CheckYourAnswerView" should {

      "render the title" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          title(view(viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the heading" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          h1(view(viewModel)) mustBe messageKey(viewModel.heading)
        }
      }

      "render the summary list keys" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          val keys = viewModel.rows.rows.map(_.key.content.asHtml.body)
          summaryListKeys(view(viewModel)) must contain theSameElementsAs keys
        }
      }

      "render the summary list values" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          val values = viewModel.rows.rows.map(_.value.content.asHtml.body)
          summaryListValues(view(viewModel)) must contain theSameElementsAs values
        }
      }

      "render the summary list actions" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          val actions =
            viewModel.rows.rows
              .flatMap(_.actions.map(_.items).getOrElse(Seq()))
              .map(AnchorTag(_))

          summaryListActions(view(viewModel)) must contain theSameElementsAs actions
        }
      }

      "render the button href" in {

        forAll(checkYourAnswersViewModelGen) { viewModel =>

          anchorButton(view(viewModel)).href mustBe viewModel.onSubmit.url
        }
      }

    }
  }
}
