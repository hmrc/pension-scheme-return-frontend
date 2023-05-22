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

package viewmodels.models

import cats.data.NonEmptyList
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.TaskListStatus.TaskListStatus

case class TaskListItemViewModel(
  link: LinkMessage,
  status: TaskListStatus
)

case class TaskListSectionViewModel(
  title: Message,
  items: NonEmptyList[TaskListItemViewModel]
)

case class TaskListViewModel(sections: NonEmptyList[TaskListSectionViewModel])

object TaskListStatus {

  sealed abstract class TaskListStatus(val description: Message)

  case object UnableToStart extends TaskListStatus("task-list.unableToStart")

  case object NotStarted extends TaskListStatus("task-list.notStarted")

  case object InProgress extends TaskListStatus("task-list.inProgress")

  case object Completed extends TaskListStatus("task-list.completed")
}
