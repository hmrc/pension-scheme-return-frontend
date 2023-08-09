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

import config.Refined.Max9999999
import models.SchemeId.Srn
import models.{ReceivedLoanType, UserAnswers}
import pages.QuestionPage
import pages.nonsipp.loansmadeoroutstanding._
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils._
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class WhoReceivedLoanPage(srn: Srn, index: Max9999999) extends QuestionPage[ReceivedLoanType] {

  override def path: JsPath = Paths.loanTransactions \ "recipientIdentityType" \ toString \ index.arrayIndex.toString

  override def toString: String = "whoReceivedLoans"

  private def pages(srn: Srn): List[Removable[_]] = pagesFirstPart(srn) ++ List(
    DatePeriodLoanPage(srn, index),
    AmountOfTheLoanPage(srn, index),
    AreRepaymentsInstalmentsPage(srn, index),
    InterestOnLoanPage(srn, index),
    OutstandingArrearsOnLoanPage(srn, index),
    SecurityGivenForLoanPage(srn, index)
  )

  private def pagesFirstPart(srn: Srn): List[Removable[_]] = List(
    IndividualRecipientNamePage(srn, index),
    IndividualRecipientNinoPage(srn, index),
    IsMemberOrConnectedPartyPage(srn, index),
    CompanyRecipientNamePage(srn, index),
    CompanyRecipientCrnPage(srn, index),
    RecipientSponsoringEmployerConnectedPartyPage(srn, index),
    PartnershipRecipientNamePage(srn, index),
    PartnershipRecipientUtrPage(srn, index),
    OtherRecipientDetailsPage(srn, index)
  )

  override def cleanup(value: Option[ReceivedLoanType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(ReceivedLoanType.Individual), Some(ReceivedLoanType.Individual)) => Try(userAnswers)
      case (Some(ReceivedLoanType.UKCompany), Some(ReceivedLoanType.UKCompany)) => Try(userAnswers)
      case (Some(ReceivedLoanType.UKPartnership), Some(ReceivedLoanType.UKPartnership)) => Try(userAnswers)
      case (Some(ReceivedLoanType.Other), Some(ReceivedLoanType.Other)) => Try(userAnswers)
      case (Some(ReceivedLoanType.Individual), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(ReceivedLoanType.UKCompany), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(ReceivedLoanType.UKPartnership), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(ReceivedLoanType.Other), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(_), _) => Try(userAnswers)
      case (None, _) => removePages(userAnswers, pages(srn))
    }
}

case class WhoReceivedLoans(srn: Srn) extends QuestionPage[Map[String, ReceivedLoanType]] {
  override def path: JsPath = Paths.loanTransactions \ "recipientIdentityType" \ toString

  override def toString: String = "whoReceivedLoans"
}
