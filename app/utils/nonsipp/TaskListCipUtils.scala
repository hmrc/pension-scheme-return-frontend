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

import play.i18n.Lang
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage._
import viewmodels.models._

object TaskListCipUtils {

  private def getCipLabel(label: String): String =
    label
      .replace(".add", ".cip")
      .replace(".change", ".cip")
      .replace(".view", ".cip")
      .replace(".declaration.incomplete", ".declaration.cip.incomplete")
      .replace(".declaration.complete", ".declaration.cip.complete")

  def transformTaskListToCipFormat(
    taskList: List[TaskListSectionViewModel],
    messagesApi: MessagesApi
  ): ListTaskListLevel1 =
    ListTaskListLevel1(
      taskList.map(
        level1 =>
          TaskListCipViewModel(
            messagesApi(
              getCipLabel(level1.title.key)
            )(Lang.defaultLang),
            level1.items.fold(
              fa =>
                if (fa.isInstanceOf[LinkMessage]) {
                  ListTaskListLevel2(
                    List(
                      TaskListLevel2(
                        messagesApi(
                          getCipLabel(fa.asInstanceOf[LinkMessage].content.key)
                        )(Lang.defaultLang()),
                        "Enabled"
                      )
                    )
                  )
                } else if (fa.isInstanceOf[Message]) {
                  ListTaskListLevel2(
                    List(
                      TaskListLevel2(
                        messagesApi(
                          getCipLabel(fa.asInstanceOf[Message].key)
                        )(Lang.defaultLang()),
                        "Disabled"
                      )
                    )
                  )
                } else {
                  ListTaskListLevel2(List(TaskListLevel2(fa.toString, ""))) //fallback
                },
              level2 =>
                ListTaskListLevel2(
                  level2.toList
                    .map(
                      item =>
                        TaskListLevel2(
                          messagesApi(
                            getCipLabel(item.link.content.key)
                          )(Lang.defaultLang),
                          messagesApi(item.status.description.key)(Lang.defaultLang())
                        )
                    )
                )
            )
          )
      )
    )
}
