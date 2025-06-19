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
import utils.ListUtils.ListTupOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.{Max300, Max50}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import viewmodels.models.SectionJourneyStatus

case class EmployerContributionsProgress(srn: Srn, memberIndex: Max300, secondaryIndex: Max50)
    extends QuestionPage[SectionJourneyStatus] {

  override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "employerContributionsProgress"
}

object EmployerContributionsProgress {
  def all(memberIndex: Max300): IndexedQuestionPage[SectionJourneyStatus] =
    new IndexedQuestionPage[SectionJourneyStatus] {

      override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString

      override def toString: String = "employerContributionsProgress"
    }

  def all(): IndexedQuestionPage[Map[String, SectionJourneyStatus]] =
    new IndexedQuestionPage[Map[String, SectionJourneyStatus]] {

      override def path: JsPath = JsPath \ toString

      override def toString: String = "employerContributionsProgress"
    }

  def exist(userAnswers: UserAnswers): Boolean =
    userAnswers.map(EmployerContributionsProgress.all()).values.exists(_.values.nonEmpty)

  implicit class EmployerContributionsUserAnswersOps(userAnswers: UserAnswers) {
    def employerContributionsProgress(memberIndex: Max300): List[(Max50, SectionJourneyStatus)] =
      userAnswers.map(EmployerContributionsProgress.all(memberIndex)).toList.refine_1[Max50.Refined]

    def employerContributionsCompleted(memberIndex: Max300): List[Max50] =
      userAnswers
        .map(EmployerContributionsProgress.all(memberIndex))
        .toList
        .refine_1[Max50.Refined]
        .collect { case (index, SectionJourneyStatus.Completed) => index }
  }
}
