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
import pages.nonsipp.landorproperty.LandPropertyInUKPage
import pages.nonsipp.loansmadeoroutstanding._
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
        List(
          DatePeriodLoanPage(srn, index),
          AmountOfTheLoanPage(srn, index),
          AreRepaymentsInstalmentsPage(srn, index),
          InterestOnLoanPage(srn, index),
          OutstandingArrearsOnLoanPage(srn, index),
          SecurityGivenForLoanPage(srn, index)
        )
      case IdentitySubject.LandOrPropertySeller =>
        List(
          LandPropertyInUKPage(srn, index)
        )
    }
  private def pagesFirstPart(srn: Srn): List[Removable[_]] =
    this.identitySubject match {
      case IdentitySubject.LoanRecipient =>
        List(
          IndividualRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          IndividualRecipientNinoPage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          IsMemberOrConnectedPartyPage(srn, index),
          CompanyRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          CompanyRecipientCrnPage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          RecipientSponsoringEmployerConnectedPartyPage(srn, index),
          PartnershipRecipientNamePage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          PartnershipRecipientUtrPage(srn, index), // TODO move this to generic page (with subject) and pass in this.identitySubject
          OtherRecipientDetailsPage(srn, index) // TODO move this to generic page (with subject) and pass in this.identitySubject
        )
      case IdentitySubject.LandOrPropertySeller => List() // TODO add land or property pages here
    }

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
      case (Some(_), _) => Try(userAnswers)
      case (None, _) => removePages(userAnswers, pages(srn))
    }
}

case class IdentityTypes(srn: Srn, identitySubject: IdentitySubject) extends QuestionPage[Map[String, IdentityType]] {
  override def path: JsPath = identitySubject match {
    case IdentitySubject.LoanRecipient => Paths.loanTransactions \ "recipientIdentityType" \ toString
    case IdentitySubject.LandOrPropertySeller =>
      JsPath \ "assets" \ "landOrProperty" \ "landOrPropertyTransactions" \ "heldPropertyTransaction" \ "propertyAcquiredFrom" \ "sellerIdentityType" \ toString
  }
  override def toString: String = "identityTypes"
}
