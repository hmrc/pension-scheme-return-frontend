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

package pages.nonsipp.landorproperty

import utils.RefinedUtils.RefinedIntOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.Max5000
import play.api.libs.json.JsPath
import viewmodels.models.SectionJourneyStatus
import pages.nonsipp.landorproperty.Paths.landOrPropertyProgress
import models.SchemeId.Srn

case class LandOrPropertyProgress(srn: Srn, index: Max5000) extends QuestionPage[SectionJourneyStatus] {

  override def path: JsPath = landOrPropertyProgress \ index.arrayIndex.toString
}

object LandOrPropertyProgress {
  def all(srn: Srn): IndexedQuestionPage[SectionJourneyStatus] =
    new IndexedQuestionPage[SectionJourneyStatus] {
      override def path: JsPath = JsPath \ toString
      override def toString: String = "landOrPropertyProgress"
    }
}
