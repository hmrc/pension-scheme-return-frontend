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

package pages.nonsipp.shares

import utils.RefinedUtils.RefinedIntOps
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import pages.nonsipp.shares.Paths.sharesProgress
import play.api.libs.json.JsPath
import viewmodels.models.SectionJourneyStatus

case class SharesProgress(srn: Srn, index: Max5000) extends QuestionPage[SectionJourneyStatus] {

  override def path: JsPath = sharesProgress \ index.arrayIndex.toString
}

object SharesProgress {
  def all(): IndexedQuestionPage[SectionJourneyStatus] =
    new IndexedQuestionPage[SectionJourneyStatus] {
      override def path: JsPath = JsPath \ toString
      override def toString: String = "sharesProgress"
    }
}
