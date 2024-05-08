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

import play.api.libs.json.{Json, Writes}

case class TaskListCipViewModel(
  name: String,
  subSections: ListTaskListLevel2
)

case class TaskListLevel2(
  name: String,
  status: String
)

case class ListTaskListLevel2(list: List[TaskListLevel2])
case class ListTaskListLevel1(list: List[TaskListCipViewModel])

object TaskListCipViewModel {

  implicit val taskListLevel2Writes: Writes[TaskListLevel2] = Json.writes[TaskListLevel2]
  implicit val writeListTaskListLevel2: Writes[ListTaskListLevel2] = list => Json.toJson(list.list)
  implicit val taskListLevel1Writes: Writes[TaskListCipViewModel] = l1 =>
    Json.obj(
      "name" -> Json.toJson(l1.name),
      "sub sections" -> Json.toJson(l1.subSections)
    )
  implicit val writeListTaskListLevel1: Writes[ListTaskListLevel1] = list =>
    Json.obj(
      "sections" -> Json.toJson(list.list)
    )
}
