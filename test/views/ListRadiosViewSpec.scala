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

import config.RefinedTypes.Max5000
import views.html.ListRadiosView
import org.scalacheck.Gen
import forms.RadioListFormProvider
import models.Pagination
import play.twirl.api.Html
import config.RefinedTypes.Max5000._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, ListRadiosViewModel, PaginatedViewModel}

class ListRadiosViewSpec extends ViewSpec with ViewBehaviours {

  runningApplication { implicit app =>
    implicit val view: ListRadiosView = injected[ListRadiosView]
    val formProvider = injected[RadioListFormProvider]
    val form = formProvider[Max5000]("summaryView.required")

    def viewModelGen(
      rows: Option[Int] = None
    ): Gen[FormPageViewModel[ListRadiosViewModel]] =
      formPageViewModelGen(using listRadiosViewModelGen(rows = rows))

    "SummaryView" - {
      act.like(renderTitle(viewModelGen())(view(form, _), _.title.key))
      act.like(renderHeading(viewModelGen())(view(form, _), _.heading))

      "render rows" in {
        forAll(viewModelGen()) { viewModel =>
          val renderedRows = radioListRows(view(form, viewModel))
          renderedRows.length mustEqual viewModel.page.rows.size
          renderedRows.map(_.selectFirst(".govuk-radios__item").text()) mustEqual viewModel.page.rows.map(row =>
            messageKey(row.text)
          )
        }
      }

      "rendering pagination elements" - {

        "show no pagination elements" - {
          "there is only 1 row and page size is 3" in {
            paginationTest(1, 1, 3) { html =>
              radioListRows(html).size mustEqual 1
              paginationElements(html).size mustEqual 0
            }
          }

          "there are 3 rows and page size is 3" in {
            paginationTest(3, 1, 3) { html =>
              radioListRows(html).size mustEqual 3
              paginationElements(html).size mustEqual 0
            }
          }
        }

        "show pagination elements" - {
          "there are 4 rows and page size is 3" in {
            paginationTest(4, 1, 3) { html =>
              radioListRows(html).size mustEqual 3
              val elems = paginationElements(html)
              elems.head.isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("1", "2", "Next")
            }
          }

          "there are 6 rows and page size is 3" in {
            paginationTest(6, 1, 3) { html =>
              radioListRows(html).size mustEqual 3
              paginationElements(html).map(_.text) mustEqual List("1", "2", "Next")
            }
          }

          "there are 7 rows and page size is 3" in {
            paginationTest(7, 1, 3) { html =>
              radioListRows(html).size mustEqual 3
              paginationElements(html).map(_.text) mustEqual List("1", "2", "3", "Next")
            }
          }

          "current page is 2, there are 7 rows and page size is 3" in {
            paginationTest(7, 2, 3) { html =>
              radioListRows(html).size mustEqual 3
              val elems = paginationElements(html)
              elems(2).isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("Previous", "1", "2", "3", "Next")
            }
          }

          "current page is 2 (final page), there are 4 rows and page size is 3" in {
            paginationTest(4, 2, 3) { html =>
              radioListRows(html).size mustEqual 1
              val elems = paginationElements(html)
              elems(2).isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("Previous", "1", "2")
            }
          }
        }
      }
    }

    def paginationTest(rows: Int, currentPage: Int, pageSize: Int)(f: Html => Unit): Unit =
      forAll(viewModelGen(rows = Some(rows))) { viewModel =>
        val pagination = Pagination(currentPage, pageSize, viewModel.page.rows.size, _ => viewModel.onSubmit)
        val paginatedViewModel =
          viewModel.copy(
            page = viewModel.page.copy(paginatedViewModel = Some(PaginatedViewModel(Message("test label"), pagination)))
          )
        val html = view(form, paginatedViewModel)

        f(html)
      }
  }
}
