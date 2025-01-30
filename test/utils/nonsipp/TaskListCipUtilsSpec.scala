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

package utils.nonsipp

import org.scalatest.matchers.must.Matchers
import controllers.TestValues
import generators.BasicGenerators
import org.scalatest.OptionValues
import generators.ModelGenerators.pensionSchemeIdGen
import models.PensionSchemeId
import play.api.i18n.MessagesApi
import play.api.test.Helpers.stubMessagesApi
import org.scalatest.freespec.AnyFreeSpec
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models._

class TaskListCipUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues with BasicGenerators {

  val pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value
  val messagesApi: MessagesApi = stubMessagesApi(
    messages = Map(
      "en" -> Map(
        "tasklist.numItems" -> "{0} {1}",
        "entities.plural" -> "entities"
      )
    )
  )

  "Transform task list to CIP" - {
    "should transform generated task list" in {
      val taskList = List(
        TaskListSectionViewModel(
          title = nonEmptyMessage.sample.value,
          item = nonEmptyInlineMessage.sample.value,
          postActionLink = nonEmptyLinkMessage.sample.value
        )
      )
      val result = TaskListCipUtils.transformTaskListToCipFormat(taskList, messagesApi)
      result.list.size mustBe 1
      result.list(0).subSections.list.size mustBe 1
    }
    "should transform task list" in {
      val taskList = List(
        TaskListSectionViewModel(
          title = Message("Section1"),
          headItem = TaskListItemViewModel(
            link = LinkMessage(
              Message("Sub-section 1-1.add", List()),
              "Url 1-1"
            ),
            status = TaskListStatus.Completed
          ),
          TaskListItemViewModel(
            link = LinkMessage(
              Message("Sub-section 1-2.change", List()),
              "Url 1-2"
            ),
            status = TaskListStatus.NotStarted
          ),
          TaskListItemViewModel(
            link = LinkMessage(
              Message("Sub-section 1-3.view", List()),
              "Url 1-3"
            ),
            status = TaskListStatus.UnableToStart
          )
        ),
        TaskListSectionViewModel(
          title = Message("Section2"),
          headItem = TaskListItemViewModel(
            link = LinkMessage(
              Message("Sub-section 2-1"),
              "Url 2-1"
            ),
            status = TaskListStatus.Recorded(2, "entities")
          )
        ),
        TaskListSectionViewModel(
          title = Message("Declaration incomplete"),
          headItem = TaskListItemViewModel(
            link = LinkMessage(
              Message("Sub-section 2-1.declaration.incomplete", List()),
              "Url 2-1"
            ),
            status = TaskListStatus.Completed
          )
        ),
        TaskListSectionViewModel(
          title = Message("Declaration complete"),
          item = Message("Declaration complete"),
          postActionLink = LinkMessage(
            Message("Sub-section 3-1.declaration.complete", List()),
            "Url 3-1"
          )
        )
      )
      val result = TaskListCipUtils.transformTaskListToCipFormat(taskList, messagesApi)
      result.list.size mustBe 4
      result.list(0).subSections.list.size mustBe 3
      result mustBe taskListInAuditEvent
    }
  }

}
