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

package pages.nonsipp.common

import config.Refined.Max5000
import models.{IdentitySubject, RecipientDetails}
import models.SchemeId.Srn
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

case class OtherRecipientDetailsPage(srn: Srn, index: Max5000, identitySubject: IdentitySubject)
    extends QuestionPage[RecipientDetails] {

  override def path: JsPath =
    identitySubject match {
      case IdentitySubject.LoanRecipient =>
        pages.nonsipp.loansmadeoroutstanding.Paths.loanTransactions \ "recipientIdentityType" \ toString \ index.arrayIndex.toString
      case IdentitySubject.LandOrPropertySeller =>
        pages.nonsipp.landorproperty.Paths.heldPropertyTransactions \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
      case IdentitySubject.SharesSeller =>
        pages.nonsipp.shares.Paths.heldSharesTransaction \ "acquiredFromType" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
      case IdentitySubject.OtherAssetSeller =>
        pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions \ "acquiredFromType" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
      case IdentitySubject.Unknown => JsPath \ "unknown" \ toString \ index.arrayIndex.toString
    }
  override def toString: String = "otherRecipientDetails"
}
