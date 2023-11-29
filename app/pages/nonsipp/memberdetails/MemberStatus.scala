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

package pages.nonsipp.memberdetails

import config.Refined.Max300
import models.SchemeId.Srn
import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps
import viewmodels.models.MemberState

case class MemberStatus(srn: Srn, index: Max300) extends QuestionPage[MemberState] {

  override def path: JsPath = Paths.personalDetails \ toString \ index.arrayIndex.toString

  override def toString: String = "memberStatus"
}

case class MemberStatuses(srn: Srn) extends QuestionPage[Map[String, MemberState]] {

  override def path: JsPath = Paths.personalDetails \ toString

  override def toString: String = "memberStatus"
}

object MemberStatusImplicits {
  implicit class MembersStatusOps(ua: UserAnswers) {
    def memberStates(srn: Srn): Map[String, MemberState] = ua.map(MemberStatuses(srn))
  }
}
