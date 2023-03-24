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

package pages

import config.Refined.Max99
import models.{NameDOB, UserAnswers}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import queries.Gettable
import utils.RefinedUtils.RefinedIntOps

case class MemberDetailsPage(srn: Srn, index: Max99) extends QuestionPage[NameDOB] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex

  override def toString: String = "memberDetailsPage"
}

case class MembersDetails(srn: Srn) extends Gettable[List[NameDOB]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "memberDetailsPage"
}

object MembersDetails {
  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): List[NameDOB] = ua.get(MembersDetails(srn)).toList.flatten
  }
}
