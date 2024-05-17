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

import models.SchemeId.Srn
import play.api.libs.json.{__, JsPath}
import config.Refined.{Max300, Max5}
import pages.QuestionPage

package object membertransferout {
  object Paths {
    val membersPayments: JsPath = __ \ "membersPayments"
    val memberDetails: JsPath = membersPayments \ "memberDetails"
    val memberTransfersOut: JsPath = memberDetails \ "memberTransfersOut"
  }

  def transferOutPages(srn: Srn, index: Max300, secondaryIndex: Max5): List[QuestionPage[_]] = List(
    ReceivingSchemeNamePage(srn, index, secondaryIndex),
    ReceivingSchemeTypePage(srn, index, secondaryIndex),
    ReportAnotherTransferOutPage(srn, index, secondaryIndex),
    TransfersOutSectionCompleted(srn, index, secondaryIndex),
    WhenWasTransferMadePage(srn, index, secondaryIndex)
  )
}
