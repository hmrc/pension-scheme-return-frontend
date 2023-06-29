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

package pages.nonsipp.loansmadeoroutstanding

import models.SchemeId.Srn
import models.{ReceivedLoanType, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils._

import scala.util.Try

case class WhoReceivedLoanPage(srn: Srn) extends QuestionPage[ReceivedLoanType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "whoReceivedLoan"

  private def individualPages(srn: Srn): List[Removable[_]] = List(
    IndividualRecipientNamePage(srn),
    IndividualRecipientNinoPage(srn),
    IsMemberOrConnectedPartyPage(srn)
  )

  private def companyPages(srn: Srn): List[Removable[_]] = List(
    CompanyRecipientNamePage(srn),
    CompanyRecipientCrnPage(srn),
    RecipientSponsoringEmployerConnectedPartyPage(srn)
  )

  private def partnershipPages(srn: Srn): List[Removable[_]] = List(
    PartnershipRecipientNamePage(srn),
    RecipientSponsoringEmployerConnectedPartyPage(srn)
  )

  private def otherPages(srn: Srn): List[Removable[_]] = List(
    OtherRecipientDetailsPage(srn)
  )

  override def cleanup(value: Option[ReceivedLoanType], userAnswers: UserAnswers): Try[UserAnswers] = value match {
    case Some(ReceivedLoanType.Individual) =>
      removePages(userAnswers, companyPages(srn) ++ partnershipPages(srn) ++ otherPages(srn))
    case Some(ReceivedLoanType.UKCompany) =>
      removePages(userAnswers, individualPages(srn) ++ partnershipPages(srn) ++ otherPages(srn))
    case Some(ReceivedLoanType.UKPartnership) =>
      removePages(userAnswers, individualPages(srn) ++ companyPages(srn) ++ otherPages(srn))
    case Some(ReceivedLoanType.Other) =>
      removePages(userAnswers, individualPages(srn) ++ companyPages(srn) ++ partnershipPages(srn))
    case None => Try(userAnswers)
  }
}
