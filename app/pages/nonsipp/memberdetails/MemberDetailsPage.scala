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

package pages.nonsipp.memberdetails

import utils.RefinedUtils.RefinedIntOps
import pages.nonsipp.employercontributions.{Paths => _}
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer.{Paths => _}
import play.api.libs.json.JsPath
import pages.nonsipp.membersurrenderedbenefits.{Paths => _}
import models.{NameDOB, UserAnswers}
import pages.nonsipp.membertransferout.{Paths => _}
import config.Refined.Max300
import pages.{IndexedQuestionPage, QuestionPage}

case class MemberDetailsPage(srn: Srn, index: Max300) extends QuestionPage[NameDOB] {

  override def path: JsPath = MembersDetailsPage.path \ index.arrayIndex.toString

  override def toString: String = MembersDetailsPage.key
}

case class MembersDetailsPages(srn: Srn) extends IndexedQuestionPage[NameDOB] {

  override def path: JsPath = MembersDetailsPage.path

  override def toString: String = MembersDetailsPage.key
}

object MembersDetailsPage {

  val key = "nameDob"
  val path: JsPath = Paths.personalDetails \ key

  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): Map[String, NameDOB] = ua.map(MembersDetailsPages(srn))
  }
}
