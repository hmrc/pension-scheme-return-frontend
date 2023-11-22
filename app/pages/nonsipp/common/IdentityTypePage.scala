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
import pages.nonsipp.landorproperty.{
  CompanySellerNamePage,
  IndividualSellerNiPage,
  LandOrPropertySellerConnectedPartyPage,
  LandPropertyIndividualSellersNamePage,
  PartnershipSellerNamePage
}
import pages.nonsipp.loansmadeoroutstanding.{
  DatePeriodLoanPage,
  IsIndividualRecipientConnectedPartyPage,
  RecipientSponsoringEmployerConnectedPartyPage,
  _
}
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils._
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class IdentityTypePage(srn: Srn, index: Max5000, identitySubject: IdentitySubject)
    extends QuestionPage[IdentityType] {

  override def path: JsPath = identitySubject match {
    case IdentitySubject.LoanRecipient =>
      Paths.loanTransactions \ "recipientIdentityType" \ toString \ index.arrayIndex.toString
    case IdentitySubject.LandOrPropertySeller =>
      JsPath \ "assets" \ "landOrProperty" \ "landOrPropertyTransactions" \ "heldPropertyTransaction" \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString \ index.arrayIndex.toString
  }

  override def toString: String = "identityTypes"

  private def pages(srn: Srn): List[Removable[_]] = pagesFirstPart(srn) ++ pagesSecondPart(srn)

  private def pagesSecondPart(srn: Srn): List[Removable[_]] =
    this.identitySubject match {
      case IdentitySubject.LoanRecipient =>
        val list = List(
          DatePeriodLoanPage(srn, index),
          AmountOfTheLoanPage(srn, index),
          AreRepaymentsInstalmentsPage(srn, index),
          InterestOnLoanPage(srn, index),
          OutstandingArrearsOnLoanPage(srn, index),
          SecurityGivenForLoanPage(srn, index)
        )
        if (index.value == 1) list :+ LoansMadeOrOutstandingPage(srn) else list

      case IdentitySubject.LandOrPropertySeller =>
        List().empty
    }
  private def pagesFirstPartGeneric(srn: Srn): List[Removable[_]] =
    List(
      IsIndividualRecipientConnectedPartyPage(srn, index),
      CompanyRecipientCrnPage(srn, index, this.identitySubject),
      PartnershipRecipientUtrPage(srn, index, this.identitySubject),
      OtherRecipientDetailsPage(srn, index, this.identitySubject)
    )
  private def pagesFirstPartSpecific(srn: Srn): List[Removable[_]] =
    this.identitySubject match {
      case IdentitySubject.LoanRecipient =>
        List(
          IndividualRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          IndividualRecipientNinoPage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          CompanyRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          PartnershipRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          RecipientSponsoringEmployerConnectedPartyPage(srn, index)
        )

      case IdentitySubject.LandOrPropertySeller =>
        List(
          LandPropertyIndividualSellersNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          IndividualSellerNiPage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          CompanySellerNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          PartnershipSellerNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          LandOrPropertySellerConnectedPartyPage(srn, index)
        )
    }
  private def pagesFirstPart(srn: Srn): List[Removable[_]] = pagesFirstPartGeneric(srn) ++ pagesFirstPartSpecific(srn)

  override def cleanup(value: Option[IdentityType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(IdentityType.Individual), Some(IdentityType.Individual)) => Try(userAnswers)
      case (Some(IdentityType.UKCompany), Some(IdentityType.UKCompany)) => Try(userAnswers)
      case (Some(IdentityType.UKPartnership), Some(IdentityType.UKPartnership)) => Try(userAnswers)
      case (Some(IdentityType.Other), Some(IdentityType.Other)) => Try(userAnswers)
      case (Some(IdentityType.Individual), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(IdentityType.UKCompany), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(IdentityType.UKPartnership), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (Some(IdentityType.Other), _) => removePages(userAnswers, pagesFirstPart(srn))
      case (None, _) => removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }
}

case class IdentityTypes(srn: Srn, identitySubject: IdentitySubject) extends QuestionPage[Map[String, IdentityType]] {
  override def path: JsPath = identitySubject match {
    case IdentitySubject.LoanRecipient => Paths.loanTransactions \ "recipientIdentityType" \ toString
    case IdentitySubject.LandOrPropertySeller =>
      pages.nonsipp.landorproperty.Paths.landOrPropertyTransactions \ "heldPropertyTransaction" \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString
  }
  override def toString: String = "identityTypes"
}
