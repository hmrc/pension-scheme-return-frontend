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
import models.Pagination
import play.api.test.FakeRequest
import play.twirl.api.Html
import viewmodels.DisplayMessage.Message
import viewmodels.models.PaginatedViewModel
import views.html.ListView

class ListViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    implicit val view: ListView = injected[ListView]
    val formProvider = injected[YesNoPageFormProvider]
    val form = formProvider("summaryView.required")

    implicit val request: FakeRequest[_] = FakeRequest()

    "SummaryView" - {
      act.like(renderTitle(summaryViewModelGen())(view(form, _), _.title.key))
      act.like(renderHeading(summaryViewModelGen())(view(form, _), _.heading))

      "render rows" in {
        forAll(summaryViewModelGen()) { viewModel =>
          val renderedRows = summaryListRows(view(form, viewModel))
          renderedRows.length mustEqual viewModel.rows.size
          renderedRows.map(_.selectFirst(".govuk-summary-list__key").text()) mustEqual viewModel.rows.map(
            row => messageKey(row.text)
          )
        }
      }

      "render hidden text" in {
        forAll(summaryViewModelGen()) { viewModel =>
          val renderedRows = summaryListRows(view(form, viewModel))
          renderedRows.length mustEqual viewModel.rows.size
          val links = renderedRows.map(_.select(".govuk-link"))
          val changeLinks = links.map(_.get(0))
          val removeLinks = links.map(_.get(1))
          changeLinks.map(_.children().select(".govuk-visually-hidden").text()) mustEqual
            viewModel.rows.map(row => messageKey(row.changeHiddenText))
          removeLinks.map(_.children().select(".govuk-visually-hidden").text()) mustEqual
            viewModel.rows.map(row => messageKey(row.removeHiddenText))
        }
      }

      "render radio button and not the inset text when showRadios is true" in {
        forAll(summaryViewModelGen()) { viewModel =>
          val radioElements = radios(view(form, viewModel))
          radioElements.size mustEqual 2
          radioElements.map(_.id) mustEqual List("value", "value-no")
          Option(inset(view(form, viewModel))) mustBe None
        }
      }

      "renders the inset text and not the radio button whenShowRadios is false" in {
        forAll(summaryViewModelGen(showRadios = false)) { viewModel =>
          radios(view(form, viewModel)).size mustEqual 0
          inset(view(form, viewModel)).text() mustEqual viewModel.insetText.key
        }
      }

      "rendering pagination elements" - {

        "show no pagination elements" - {
          "there is only 1 row and page size is 3" in {
            paginationTest(1, 1, 3) { html =>
              summaryListRows(html).size mustEqual 1
              paginationElements(html).size mustEqual 0
            }
          }

          "there are 3 rows and page size is 3" in {
            paginationTest(3, 1, 3) { html =>
              summaryListRows(html).size mustEqual 3
              paginationElements(html).size mustEqual 0
            }
          }
        }

        "show pagination elements" - {
          "there are 4 rows and page size is 3" in {
            paginationTest(4, 1, 3) { html =>
              summaryListRows(html).size mustEqual 3
              val elems = paginationElements(html)
              elems.head.isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("1", "2", "Next")
            }
          }

          "there are 6 rows and page size is 3" in {
            paginationTest(6, 1, 3) { html =>
              summaryListRows(html).size mustEqual 3
              paginationElements(html).map(_.text) mustEqual List("1", "2", "Next")
            }
          }

          "there are 7 rows and page size is 3" in {
            paginationTest(7, 1, 3) { html =>
              summaryListRows(html).size mustEqual 3
              paginationElements(html).map(_.text) mustEqual List("1", "2", "3", "Next")
            }
          }

          "current page is 2, there are 7 rows and page size is 3" in {
            paginationTest(7, 2, 3) { html =>
              summaryListRows(html).size mustEqual 3
              val elems = paginationElements(html)
              elems(2).isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("Previous", "1", "2", "3", "Next")
            }
          }

          "current page is 2 (final page), there are 4 rows and page size is 3" in {
            paginationTest(4, 2, 3) { html =>
              summaryListRows(html).size mustEqual 1
              val elems = paginationElements(html)
              elems(2).isCurrentPage mustEqual true
              elems.map(_.text) mustEqual List("Previous", "1", "2")
            }
          }
        }
      }
    }

    def paginationTest(rows: Int, currentPage: Int, pageSize: Int)(f: Html => Unit): Unit =
      forAll(summaryViewModelGen(rows)) { viewModel =>
        val pagination = Pagination(currentPage, pageSize, viewModel.rows.size, _ => viewModel.onSubmit)
        val paginatedViewModel =
          viewModel.copy(paginatedViewModel = Some(PaginatedViewModel(Message("test label"), pagination)))
        val html = view(form, paginatedViewModel)

        f(html)
      }
  }
}
