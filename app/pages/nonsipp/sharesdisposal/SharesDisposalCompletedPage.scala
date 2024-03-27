/*
 * Copyright 2023 HM Revenue & Customs
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

package pages.nonsipp.sharesdisposal

import utils.RefinedUtils.RefinedIntOps
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import viewmodels.models.SectionCompleted
import config.Refined.{Max50, Max5000}
import pages.{IndexedQuestionPage, QuestionPage}

case class SharesDisposalCompletedPage(srn: Srn, shareIndex: Max5000, disposalIndex: Max50)
    extends QuestionPage[SectionCompleted.type] {

  override def path: JsPath =
    Paths.disposedSharesTransaction \ toString \ shareIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "sharesDisposalCompleted"
}

object SharesDisposalCompleted {
  def all(
    srn: Srn,
    shareIndex: Max5000
  ): IndexedQuestionPage[SectionCompleted.type] = new IndexedQuestionPage[SectionCompleted.type] {

    override def path: JsPath =
      Paths.disposedSharesTransaction \ toString \ shareIndex.arrayIndex.toString

    override def toString: String = "sharesDisposalCompleted"
  }
}

case class SharesDisposalCompletedPages(srn: Srn) extends IndexedQuestionPage[Map[String, SectionCompleted.type]] {

  override def path: JsPath = Paths.disposedSharesTransaction \ toString

  override def toString: String = "sharesDisposalCompleted"
}
