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

import utils.RefinedUtils._
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes._
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import viewmodels.models.SectionCompleted

case class MemberDetailsCompletedPage(srn: Srn, index: Max300) extends QuestionPage[SectionCompleted] {

  override def path: JsPath = Paths.personalDetails \ toString \ index.arrayIndex.toString

  override def toString: String = MemberDetailsCompletedPage.key
}

case class MembersDetailsCompletedPages(srn: Srn) extends IndexedQuestionPage[SectionCompleted] {

  override def path: JsPath = Paths.personalDetails \ toString

  override def toString: String = MemberDetailsCompletedPage.key
}

object MemberDetailsCompletedPage {
  val key = "memberDetailsSectionCompleted"
}
