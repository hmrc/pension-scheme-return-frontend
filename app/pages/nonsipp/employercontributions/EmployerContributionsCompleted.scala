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

package pages.nonsipp.employercontributions

import utils.RefinedUtils.RefinedIntOps
import utils.ListUtils.ListOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.{Max300, Max50}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import viewmodels.models.SectionCompleted

case class EmployerContributionsCompleted(srn: Srn, memberIndex: Max300, secondaryIndex: Max50)
    extends QuestionPage[SectionCompleted.type] {

  override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "employerContributionsCompleted"
}

object EmployerContributionsCompleted {
  def all(): IndexedQuestionPage[Map[String, SectionCompleted.type]] =
    new IndexedQuestionPage[Map[String, SectionCompleted.type]] {

      override def path: JsPath = JsPath \ toString

      override def toString: String = "employerContributionsCompleted"
    }

  def all(memberIndex: Max300): IndexedQuestionPage[SectionCompleted.type] =
    new IndexedQuestionPage[SectionCompleted.type] {

      override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString

      override def toString: String = "employerContributionsCompleted"
    }

  implicit class EmployerContributionsUserAnswersOps(userAnswers: UserAnswers) {
    def employerContributionsCompleted(memberIndex: Max300): List[Max50] =
      userAnswers.map(EmployerContributionsCompleted.all(memberIndex)).keys.toList.refine[Max50.Refined]
  }
}
