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

import play.api.data.Forms.{mapping, text}
import views.html.DateRangeView
import play.api.data
import models.DateRange
import viewmodels.models.DateRangeViewModel

import java.time.LocalDate

class DateRangeViewSpec extends ViewSpec with ViewBehaviours {

  runningApplication { implicit app =>
    val view = injected[DateRangeView]

    def localDateMapping(error: String) =
      text.verifying(error, _.nonEmpty).transform[LocalDate](LocalDate.parse, _.toString)

    val dateRangeForm =
      data.Form(
        mapping(
          "dates" -> mapping(
            "startDate" -> localDateMapping("startDate.required"),
            "endDate" -> localDateMapping("endDate.required")
          )(DateRange.apply)(x => Some(x._1, x._2))
        )(identity)(Some(_))
      )
    val invalidForm = dateRangeForm.bind(Map("dates.startDate" -> "", "dates.endDate" -> ""))

    val viewModelGen = formPageViewModelGen[DateRangeViewModel]

    "DateRangeView" - {

      act.like(renderTitle(viewModelGen)(view(dateRangeForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(dateRangeForm, _), _.heading))
      act.like(renderDescription(viewModelGen)(view(dateRangeForm, _), _.description))
      act.like(renderButtonText(viewModelGen)(view(dateRangeForm, _), _.buttonText))
      act.like(renderForm(viewModelGen)(view(dateRangeForm, _), _.onSubmit))

      act.like {
        renderErrors(viewModelGen)(view(invalidForm, _), _ => "startDate.required")
          .withName("render startDate errors")
      }

      act.like {
        renderErrors(viewModelGen)(view(invalidForm, _), _ => "endDate.required")
          .withName("render endDate errors")
      }

      "render the start date label" in {

        forAll(viewModelGen) { viewModel =>
          legend(view(dateRangeForm, viewModel)) must contain(viewModel.page.startDateLabel.key)
        }
      }

      "render the end date label" in {

        forAll(viewModelGen) { viewModel =>
          legend(view(dateRangeForm, viewModel)) must contain(viewModel.page.endDateLabel.key)
        }
      }
    }
  }
}
