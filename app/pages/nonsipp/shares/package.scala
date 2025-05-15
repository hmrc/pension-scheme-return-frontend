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
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import play.api.libs.json.{__, JsPath}
import pages.nonsipp.common._
import models.IdentitySubject.SharesSeller

package object shares {

  object Paths {
    val shares: JsPath = __ \ "shares"
    val sharesProgress: JsPath = __ \ "sharesProgress"
    val shareTransactions: JsPath = shares \ "shareTransactions"
    val heldSharesTransaction: JsPath = shareTransactions \ "heldSharesTransaction"
    val shareIdentification: JsPath = shareTransactions \ "shareIdentification"

  }

  def sharesPages(srn: Srn, index: Max5000, isLastRecord: Boolean): List[QuestionPage[_]] = {
    val list = List(
      ClassOfSharesPage(srn, index),
      CompanyNameOfSharesSellerPage(srn, index),
      CompanyNameRelatedSharesPage(srn, index),
      CostOfSharesPage(srn, index),
      HowManySharesPage(srn, index),
      IndividualNameOfSharesSellerPage(srn, index),
      PartnershipShareSellerNamePage(srn, index),
      RemoveSharesPage(srn, index),
      SharesCompanyCrnPage(srn, index),
      SharesFromConnectedPartyPage(srn, index),
      SharesIndependentValuationPage(srn, index),
      SharesIndividualSellerNINumberPage(srn, index),
      SharesTotalIncomePage(srn, index),
      TotalAssetValuePage(srn, index),
      TypeOfSharesHeldPage(srn, index),
      WhenDidSchemeAcquireSharesPage(srn, index),
      WhyDoesSchemeHoldSharesPage(srn, index),
      SharesCompleted(srn, index),
      IdentityTypePage(srn, index, SharesSeller),
      CompanyRecipientCrnPage(srn, index, SharesSeller),
      OtherRecipientDetailsPage(srn, index, SharesSeller),
      PartnershipRecipientUtrPage(srn, index, SharesSeller)
    )
    if (isLastRecord) list :+ DidSchemeHoldAnySharesPage(srn) else list
  }
}
