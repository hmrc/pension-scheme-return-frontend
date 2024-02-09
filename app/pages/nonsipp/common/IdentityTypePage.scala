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
import models.SchemeId.Srn
import models.{IdentitySubject, IdentityType, UserAnswers}
import pages.QuestionPage
import pages.nonsipp.landorproperty._
import pages.nonsipp.loansmadeoroutstanding
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.shares._
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils._
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class IdentityTypePage(srn: Srn, index: Max5000, identitySubject: IdentitySubject)
    extends QuestionPage[IdentityType] {

  override def path: JsPath = identitySubject match {
    case IdentitySubject.LoanRecipient =>
      loansmadeoroutstanding.Paths.loanTransactions \ "recipientIdentityType" \ toString \ index.arrayIndex.toString
    case IdentitySubject.LandOrPropertySeller =>
      pages.nonsipp.landorproperty.Paths.heldPropertyTransactions \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
    case IdentitySubject.SharesSeller =>
      pages.nonsipp.shares.Paths.heldSharesTransaction \ "acquiredFromType" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
  }

  override def toString: String = "identityTypes"

  private def allPages(srn: Srn, userAnswers: UserAnswers): List[Removable[_]] =
    pagesDependentOnIdentitySubject(srn) ++ pagesNotDependentOnIdentitySubject(
      srn,
      userAnswers.map(IdentityTypes(srn, IdentitySubject.LoanRecipient)).size == 1
    )

  private def pagesNotDependentOnIdentitySubject(srn: Srn, isLastRecord: Boolean): List[Removable[_]] =
    this.identitySubject match {
      // only for loans for now, as this is the first question in the journey
      case IdentitySubject.LoanRecipient =>
        val list = List(
          DatePeriodLoanPage(srn, index),
          AmountOfTheLoanPage(srn, index),
          AreRepaymentsInstalmentsPage(srn, index),
          InterestOnLoanPage(srn, index),
          OutstandingArrearsOnLoanPage(srn, index),
          SecurityGivenForLoanPage(srn, index)
        )
        if (isLastRecord) list :+ LoansMadeOrOutstandingPage(srn) else list
      case IdentitySubject.LandOrPropertySeller =>
        List().empty
      case IdentitySubject.SharesSeller =>
        List().empty
    }
  private def genericPagesDependentOnIdentitySubject(srn: Srn): List[Removable[_]] =
    List(
      IsIndividualRecipientConnectedPartyPage(srn, index),
      CompanyRecipientCrnPage(srn, index, this.identitySubject),
      PartnershipRecipientUtrPage(srn, index, this.identitySubject),
      OtherRecipientDetailsPage(srn, index, this.identitySubject)
    )
  private def pagesNotUsingIdentitySubject(srn: Srn): List[Removable[_]] =
    this.identitySubject match {
      case IdentitySubject.LoanRecipient =>
        List(
          IndividualRecipientNamePage(srn, index),
          IndividualRecipientNinoPage(srn, index),
          CompanyRecipientNamePage(srn, index),
          PartnershipRecipientNamePage(srn, index),
          RecipientSponsoringEmployerConnectedPartyPage(srn, index)
        )

      case IdentitySubject.LandOrPropertySeller =>
        List(
          LandPropertyIndividualSellersNamePage(srn, index),
          IndividualSellerNiPage(srn, index),
          CompanySellerNamePage(srn, index),
          PartnershipSellerNamePage(srn, index),
          LandOrPropertySellerConnectedPartyPage(srn, index)
        )

      case IdentitySubject.SharesSeller =>
        List(
          IndividualNameOfSharesSellerPage(srn, index),
          SharesIndividualSellerNINumberPage(srn, index),
          CompanyNameOfSharesSellerPage(srn, index),
          PartnershipShareSellerNamePage(srn, index)
        )

      case _ =>
        List().empty
    }
  private def pagesDependentOnIdentitySubject(srn: Srn): List[Removable[_]] =
    genericPagesDependentOnIdentitySubject(srn) ++ pagesNotUsingIdentitySubject(srn)

  override def cleanup(value: Option[IdentityType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(IdentityType.Individual), Some(IdentityType.Individual)) => Try(userAnswers)
      case (Some(IdentityType.UKCompany), Some(IdentityType.UKCompany)) => Try(userAnswers)
      case (Some(IdentityType.UKPartnership), Some(IdentityType.UKPartnership)) => Try(userAnswers)
      case (Some(IdentityType.Other), Some(IdentityType.Other)) => Try(userAnswers)
      case (Some(IdentityType.Individual), _) => removePages(userAnswers, pagesDependentOnIdentitySubject(srn))
      case (Some(IdentityType.UKCompany), _) => removePages(userAnswers, pagesDependentOnIdentitySubject(srn))
      case (Some(IdentityType.UKPartnership), _) => removePages(userAnswers, pagesDependentOnIdentitySubject(srn))
      case (Some(IdentityType.Other), _) => removePages(userAnswers, pagesDependentOnIdentitySubject(srn))
      case (None, _) => removePages(userAnswers, allPages(srn, userAnswers))
      case _ => Try(userAnswers)
    }
}

case class IdentityTypes(srn: Srn, identitySubject: IdentitySubject) extends QuestionPage[Map[String, IdentityType]] {
  override def path: JsPath = identitySubject match {
    case IdentitySubject.LoanRecipient =>
      loansmadeoroutstanding.Paths.loanTransactions \ "recipientIdentityType" \ toString
    case IdentitySubject.LandOrPropertySeller =>
      pages.nonsipp.landorproperty.Paths.heldPropertyTransactions \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString
    case IdentitySubject.SharesSeller =>
      pages.nonsipp.shares.Paths.heldSharesTransaction \ "acquiredFromType" \ "sellerIdentityType" \ toString
  }
  override def toString: String = "identityTypes"
}
