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

package pages.nonsipp

import pages.QuestionPage
import config.RefinedTypes.{Max300, Max5}
import models.SchemeId.Srn
import play.api.libs.json.{__, JsPath}

package object receivetransfer {

  object Paths {
    val membersPayments: JsPath = __ \ "membersPayments"
    val memberDetails: JsPath = membersPayments \ "memberDetails"
    val memberTransfersIn: JsPath = memberDetails \ "memberTransfersIn"
  }

  def transferInPages(srn: Srn, index: Max300, secondaryIndex: Max5): List[QuestionPage[_]] = List(
    DidTransferIncludeAssetPage(srn, index, secondaryIndex),
    ReportAnotherTransferInPage(srn, index, secondaryIndex),
    TotalValueTransferPage(srn, index, secondaryIndex),
    TransferringSchemeNamePage(srn, index, secondaryIndex),
    TransferringSchemeTypePage(srn, index, secondaryIndex),
    TransfersInSectionCompleted(srn, index, secondaryIndex),
    WhenWasTransferReceivedPage(srn, index, secondaryIndex),
    ReceiveTransferProgress(srn, index, secondaryIndex)
  )
}
