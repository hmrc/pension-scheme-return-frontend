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

package pages.nonsipp.landorpropertydisposal

import utils.RefinedUtils.RefinedIntOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.{Max50, Max5000}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import viewmodels.models.SectionCompleted
case class LandPropertyDisposalCompletedPage(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50)
    extends QuestionPage[SectionCompleted.type] {
  override def path: JsPath =
    Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "disposalCompleted"
}

object LandPropertyDisposalCompleted {
  def all(landOrPropertyIndex: Max5000): IndexedQuestionPage[SectionCompleted.type] =
    new IndexedQuestionPage[SectionCompleted.type] {
      override def path: JsPath =
        Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString

      override def toString: String = "disposalCompleted"
    }
}

case class LandPropertyDisposalCompletedPages(srn: Srn)
    extends IndexedQuestionPage[Map[String, SectionCompleted.type]] {
  override def path: JsPath = Paths.disposalPropertyTransaction \ toString

  override def toString: String = "disposalCompleted"
}
