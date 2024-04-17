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

package pages.nonsipp.otherassetsdisposal

import utils.RefinedUtils.RefinedIntOps
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import viewmodels.models.SectionJourneyStatus
import config.Refined.{Max50, Max5000}
import pages.{IndexedQuestionPage, QuestionPage}

case class OtherAssetsDisposalProgress(srn: Srn, assetIndex: Max5000, disposalIndex: Max50)
    extends QuestionPage[SectionJourneyStatus] {

  override def path: JsPath =
    Paths.otherAssetDisposalTransactions \ toString \ assetIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "otherAssetsDisposalCompleted"
}

object OtherAssetsDisposalProgress {
  def all(srn: Srn, assetIndex: Max5000): IndexedQuestionPage[SectionJourneyStatus] =
    new IndexedQuestionPage[SectionJourneyStatus] {

      override def path: JsPath = Paths.otherAssetDisposalTransactions \ toString \ assetIndex.arrayIndex.toString

      override def toString: String = "otherAssetsDisposalCompleted"
    }

  def all(srn: Srn): IndexedQuestionPage[Map[String, SectionJourneyStatus]] =
    new IndexedQuestionPage[Map[String, SectionJourneyStatus]] {

      override def path: JsPath = Paths.otherAssetDisposalTransactions \ toString

      override def toString: String = "otherAssetsDisposalCompleted"
    }
}
