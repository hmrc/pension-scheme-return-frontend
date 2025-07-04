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

import utils.RefinedUtils._
import utils.ListUtils.ListOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes._
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import viewmodels.models.SectionCompleted

case class TransfersOutSectionCompleted(srn: Srn, index: Max300, secondaryIndex: Max5)
    extends QuestionPage[SectionCompleted.type] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "transfersOutCYA"
}

case class TransfersOutCompletedPages(srn: Srn, index: Max300) extends IndexedQuestionPage[SectionCompleted.type] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString

  override def toString: String = "transfersOutCYA"
}

object TransfersOutSectionCompleted {

  def all(): IndexedQuestionPage[Map[String, SectionCompleted.type]] =
    new IndexedQuestionPage[Map[String, SectionCompleted.type]] {

      override def path: JsPath = JsPath \ toString

      override def toString: String = "transfersOutCYA"
    }

  def all(index: Max300): IndexedQuestionPage[Map[String, SectionCompleted.type]] =
    new IndexedQuestionPage[Map[String, SectionCompleted.type]] {

      override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString

      override def toString: String = "transfersOutCYA"
    }

  def exists(userAnswers: UserAnswers): Boolean = userAnswers.map(all()).values.exists(_.values.nonEmpty)

  implicit class TransfersOutSectionCompletedUserAnswersOps(ua: UserAnswers) {
    def transfersOutSectionCompleted(srn: Srn, index: Max300): List[Max5] =
      ua.map(TransfersOutCompletedPages(srn, index))
        .toList
        .collect { case (i, _) =>
          i
        }
        .refine[Max5.Refined]
  }
}
