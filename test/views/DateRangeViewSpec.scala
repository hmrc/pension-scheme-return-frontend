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

import forms.DateRangeFormProvider
import forms.mappings.DateFormErrors
import play.api.test.FakeRequest
import views.html.DateRangeView

class DateRangeViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[DateRangeView]

    implicit val request = FakeRequest()

    val dateErrors = DateFormErrors("required", "day", "month", "year", "invalid", "chars")
    val dateRangeForm = new DateRangeFormProvider()(
      dateErrors.copy(required = "startDate.required"),
      dateErrors.copy(required = "endDate.required")
    )


    "DateRangeView" should {

      "render the title" in {

        forAll(dateRangeViewModelGen) { viewModel =>
          title(view(dateRangeForm, viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the heading" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          h1(view(dateRangeForm, viewModel)) mustBe viewModel.heading.key
        }
      }

      "render the description" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          whenever(viewModel.description.nonEmpty) {

            p(view(dateRangeForm, viewModel)) must contain(viewModel.description.value.key)
          }
        }
      }

      "render the start date label" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          legend(view(dateRangeForm, viewModel)) must contain(viewModel.startDateLabel.key)
        }
      }

      "render the end date label" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          legend(view(dateRangeForm, viewModel)) must contain(viewModel.endDateLabel.key)
        }
      }

      "have form" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          form(view(dateRangeForm, viewModel)).method mustBe viewModel.onSubmit.method
          form(view(dateRangeForm, viewModel)).action mustBe viewModel.onSubmit.url
        }
      }

      "render the start date required error summary" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]())
          errorSummary(view(invalidForm, viewModel)).text() must include("startDate.required")
        }
      }

      "render the start date required error message" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]())
          errorMessage(view(invalidForm, viewModel)).text() must include("startDate.required")
        }
      }

      "render the end date required error summary" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]())
          errorSummary(view(invalidForm, viewModel)).text() must include("endDate.required")
        }
      }

      "render the end date required error message" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]())
          errorMessage(view(invalidForm, viewModel)).text() must include("endDate.required")
        }
      }
    }
  }
}