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

package viewmodels.models

import viewmodels.implicits._
import cats.data.NonEmptyList
import viewmodels.models.TaskListStatus.TaskListStatus
import viewmodels.DisplayMessage._

case class TaskListItemViewModel(
  link: LinkMessage,
  status: TaskListStatus
)

case class TaskListSectionViewModel(
  title: Message,
  items: Either[InlineMessage, NonEmptyList[TaskListItemViewModel]],
  postActionLink: Option[LinkMessage]
)

object TaskListSectionViewModel {

  def apply(
    title: Message,
    headItem: TaskListItemViewModel,
    tailItems: TaskListItemViewModel*
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, Right(NonEmptyList(headItem, tailItems.toList)), None)

  def apply(
    title: Message,
    item: InlineMessage,
    postActionLink: LinkMessage
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, Left(item), Some(postActionLink))
}

case class TaskListViewModel(
  displayNotSubmittedMessage: Boolean,
  hasHistory: Boolean,
  historyLink: Option[LinkMessage],
  submissionDateMessage: Message,
  sections: NonEmptyList[TaskListSectionViewModel]
)

object TaskListViewModel {

  def apply(
    displayNotSubmittedMessage: Boolean,
    hasHistory: Boolean,
    historyLink: Option[LinkMessage],
    submissionDateMessage: Message,
    headItem: TaskListSectionViewModel,
    tailItems: TaskListSectionViewModel*
  ): TaskListViewModel = TaskListViewModel(
    displayNotSubmittedMessage,
    hasHistory,
    historyLink,
    submissionDateMessage,
    NonEmptyList(headItem, tailItems.toList)
  )
}

object TaskListStatus {

  sealed abstract class TaskListStatus(val description: Message)

  case object UnableToStart extends TaskListStatus("tasklist.unableToStart")

  case object NotStarted extends TaskListStatus("tasklist.notStarted")

  case object InProgress extends TaskListStatus("tasklist.inProgress")

  case object Completed extends TaskListStatus("tasklist.completed")

  case object Updated extends TaskListStatus("tasklist.updated")

  case object Check extends TaskListStatus("tasklist.check")

  case object Recorded extends TaskListStatus("tasklist.recorded")

  case class Recorded(numRecorded: Int, itemKey: String)
      extends TaskListStatus(
        numRecorded match {
          case 0 => Message("tasklist.noneRecorded")
          case 1 => Message("tasklist.numItems", numRecorded, itemKey ++ ".singular")
          case _ => Message("tasklist.numItems", numRecorded, itemKey ++ ".plural")
        }
      )
}
