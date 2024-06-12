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

package pages.nonsipp.membersurrenderedbenefits

import utils.RefinedUtils._
import utils.ListUtils.ListOps
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import viewmodels.models.SectionCompleted
import config.Refined._
import pages.{IndexedQuestionPage, QuestionPage}

case class SurrenderedBenefitsCompletedPage(srn: Srn, index: Max300) extends QuestionPage[SectionCompleted] {

  override def path: JsPath = Paths.memberPensionSurrender \ toString \ index.arrayIndex.toString

  override def toString: String = "surrenderedBenefitsSectionCompleted"
}

object SurrenderedBenefitsCompleted {
  def all(srn: Srn): IndexedQuestionPage[SectionCompleted] = new IndexedQuestionPage[SectionCompleted] {

    override def path: JsPath = Paths.memberPensionSurrender \ toString

    override def toString: String = "surrenderedBenefitsSectionCompleted"
  }

  implicit class SurrenderedBenefitsUserAnswersOps(ua: UserAnswers) {
    def surrenderedBenefitsCompleted(srn: Srn): List[Max300] =
      ua.map(SurrenderedBenefitsCompleted.all(srn))
        .toList
        .collect {
          case (index, _) => index
        }
        .refine[Max300.Refined]
  }
}
