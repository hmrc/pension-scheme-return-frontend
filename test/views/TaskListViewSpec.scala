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

import play.api.test.FakeRequest
import viewmodels.models.TaskListViewModel
import views.html.TaskListView

class TaskListViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[TaskListView]

    implicit val request = FakeRequest()

    val viewModelGen = pageViewModelGen[TaskListViewModel]

    "TaskListView" - {

      act.like(renderTitle(viewModelGen)(view(_), _.title.key))
      act.like(renderHeading(viewModelGen)(view(_), _.heading))
      act.like(renderDescription(viewModelGen)(view(_), _.description))

      "render task list section headers" in {
        forAll(viewModelGen) { viewmodel =>
          val expected =
            viewmodel.page.sections.zipWithIndex.map {
              case (section, index) =>
                s"${index + 1}. ${renderMessage(section.title)}"
            }.toList

          h2(view(viewmodel)) must contain allElementsOf expected
        }
      }

      "render all task list links" in {

        forAll(viewModelGen) { viewmodel =>
          val expected =
            viewmodel.page.sections.flatMap(_.items.map(i => AnchorTag(i.link))).toList

          anchors(view(viewmodel)) must contain allElementsOf expected
        }
      }

      "render all task list statuses" in {

        forAll(viewModelGen) { viewmodel =>
          val expected =
            viewmodel.page.sections.flatMap(_.items.map(i => renderMessage(i.status.description).body)).toList

          span(view(viewmodel)) must contain allElementsOf expected
        }
      }
    }
  }
}
