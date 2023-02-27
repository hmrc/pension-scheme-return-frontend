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

import models.DateRange
import play.api.data
import play.api.data.Forms.{mapping, text}
import play.api.test.FakeRequest
import views.html.DateRangeView

import java.time.LocalDate

class DateRangeViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[DateRangeView]

    implicit val request = FakeRequest()

    def localDateMapping(error: String) =
      text.verifying(error, _.nonEmpty).transform[LocalDate](LocalDate.parse, _.toString)

    val dateRangeForm =
      data.Form(
        mapping(
          "dates" -> mapping(
            "startDate" -> localDateMapping("startDate.required"),
            "endDate"   -> localDateMapping("endDate.required")
          )(DateRange.apply)(DateRange.unapply)
        )(identity)(Some(_))
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

          val invalidForm = dateRangeForm.bind(Map[String, String]("dates.startDate" -> ""))
          errorSummary(view(invalidForm, viewModel)).text() must include("startDate.required")
        }
      }

      "render the start date required error message" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]("dates.startDate" -> ""))
          errorMessage(view(invalidForm, viewModel)).text() must include("startDate.required")
        }
      }

      "render the end date required error summary" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]("dates.endDate" -> ""))
          errorSummary(view(invalidForm, viewModel)).text() must include("endDate.required")
        }
      }

      "render the end date required error message" in {

        forAll(dateRangeViewModelGen) { viewModel =>

          val invalidForm = dateRangeForm.bind(Map[String, String]("dates.endDate" -> ""))
          errorMessage(view(invalidForm, viewModel)).text() must include("endDate.required")
        }
      }
    }
  }
}