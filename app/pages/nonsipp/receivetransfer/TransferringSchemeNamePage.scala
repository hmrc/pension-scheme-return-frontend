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

package pages.nonsipp.receivetransfer

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import pages.QuestionPage
import config.RefinedTypes.{Max300, Max5}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers

import scala.util.Try

case class TransferringSchemeNamePage(srn: Srn, memberIndex: Max300, index: Max5) extends QuestionPage[String] {

  override def path: JsPath =
    Paths.memberTransfersIn \ toString \ memberIndex.arrayIndex.toString \ index.arrayIndex.toString

  override def toString: String = "schemeName"

  override def cleanup(value: Option[String], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) =>
        removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn): List[Removable[_]] =
    List(
      TransferringSchemeTypePage(srn, memberIndex, index),
      TotalValueTransferPage(srn, memberIndex, index),
      WhenWasTransferReceivedPage(srn, memberIndex, index),
      DidTransferIncludeAssetPage(srn, memberIndex, index),
      ReportAnotherTransferInPage(srn, memberIndex, index),
      TransfersInSectionCompleted(srn, memberIndex, index),
      ReceiveTransferProgress(srn, memberIndex, index)
    )
}

case class TransferringSchemeNamePages(srn: Srn, memberIndex: Max300) extends QuestionPage[Map[String, String]] {
  override def path: JsPath =
    Paths.memberTransfersIn \ toString \ memberIndex.arrayIndex.toString

  override def toString: String = "schemeName"
}
