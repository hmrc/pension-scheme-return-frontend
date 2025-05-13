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

package pages.nonsipp.membertransferout

import utils.RefinedUtils.RefinedIntOps
import utils.ListUtils.ListTupOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.{Max300, Max5}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import viewmodels.models.SectionJourneyStatus

case class MemberTransferOutProgress(srn: Srn, memberIndex: Max300, secondaryIndex: Max5)
    extends QuestionPage[SectionJourneyStatus] {

  override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "memberTransferOutProgress"
}

object MemberTransferOutProgress {
  def all(srn: Srn, memberIndex: Max300): IndexedQuestionPage[SectionJourneyStatus] =
    new IndexedQuestionPage[SectionJourneyStatus] {

      override def path: JsPath = JsPath \ toString \ memberIndex.arrayIndex.toString

      override def toString: String = "memberTransferOutProgress"
    }

  def all(srn: Srn): IndexedQuestionPage[Map[String, SectionJourneyStatus]] =
    new IndexedQuestionPage[Map[String, SectionJourneyStatus]] {

      override def path: JsPath = JsPath \ toString

      override def toString: String = "memberTransferOutProgress"
    }

  def exist(srn: Srn, userAnswers: UserAnswers): Boolean =
    userAnswers.map(MemberTransferOutProgress.all(srn)).values.exists(_.values.nonEmpty)

  implicit class TransfersOutSectionCompletedUserAnswersOps(userAnswers: UserAnswers) {

    def memberTransferOutProgress(srn: Srn, memberIndex: Max300): List[(Max5, SectionJourneyStatus)] =
      userAnswers.map(MemberTransferOutProgress.all(srn, memberIndex)).toList.refine_1[Max5.Refined]

    def transfersOutSectionCompleted(srn: Srn, memberIndex: Max300): List[Max5] =
      userAnswers
        .map(MemberTransferOutProgress.all(srn, memberIndex))
        .toList
        .refine_1[Max5.Refined]
        .collect { case (index, SectionJourneyStatus.Completed) => index }
  }

}
