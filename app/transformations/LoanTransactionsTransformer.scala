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

package transformations

import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple3Semigroupal}
import com.google.inject.Singleton
import config.Refined.OneTo5000
import eu.timepit.refined.refineV
import models.ConditionalYesNo._
import models.IdentityType.reads
import models.SchemeId.Srn
import models.SponsoringOrConnectedParty.ConnectedParty
import models.requests.DataRequest
import models.requests.psr._
import models.{IdentitySubject, IdentityType}
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypes,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.loansmadeoroutstanding._

import javax.inject.Inject

@Singleton()
class LoanTransactionsTransformer @Inject()() {

  private type OptionalRecipientDetails = Option[(String, RecipientIdentityType, Boolean, Option[String])]

  def transform(srn: Srn)(implicit request: DataRequest[_]): List[LoanTransactions] = {
    request.userAnswers
      .map(IdentityTypes(srn, IdentitySubject.LoanRecipient))
      .map {
        case (key, identityType) =>
          key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
            case None => None
            case Some(index) =>
              val optRecipientIdentityDetails: OptionalRecipientDetails = identityType match {
                case IdentityType.Individual =>
                  (
                    request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
                    request.userAnswers.get(IndividualRecipientNinoPage(srn, index)).map(_.value),
                    request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index))
                  ).mapN {
                    case (name, Left(noNinoReason), connectedParty) =>
                      (
                        name,
                        RecipientIdentityType(IdentityType.Individual, None, Some(noNinoReason), None),
                        connectedParty,
                        None
                      )
                    case (name, Right(nino), connectedParty) =>
                      (
                        name,
                        RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
                        connectedParty,
                        None
                      )
                  }
                case IdentityType.UKCompany =>
                  val recipientSponsoringEmployer =
                    request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                  (
                    request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
                    request.userAnswers
                      .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
                      .map(_.value)
                  ).mapN {
                    case (name, Left(noCrnReason)) =>
                      (
                        name,
                        RecipientIdentityType(IdentityType.UKCompany, None, Some(noCrnReason), None),
                        recipientSponsoringEmployer.contains(ConnectedParty),
                        recipientSponsoringEmployer.map(_.name)
                      )
                    case (name, Right(crn)) =>
                      (
                        name,
                        RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
                        recipientSponsoringEmployer.contains(ConnectedParty),
                        recipientSponsoringEmployer.map(_.name)
                      )
                  }
                case IdentityType.UKPartnership =>
                  val recipientSponsoringEmployer =
                    request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                  (
                    request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
                    request.userAnswers
                      .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
                      .map(_.value)
                  ).mapN {
                    case (name, Left(noUtrReason)) =>
                      (
                        name,
                        RecipientIdentityType(IdentityType.UKPartnership, None, Some(noUtrReason), None),
                        recipientSponsoringEmployer.contains(ConnectedParty),
                        recipientSponsoringEmployer.map(_.name)
                      )
                    case (name, Right(utr)) =>
                      (
                        name,
                        RecipientIdentityType(
                          IdentityType.UKPartnership,
                          Some(utr.value.filterNot(_.isWhitespace)),
                          None,
                          None
                        ),
                        recipientSponsoringEmployer.contains(ConnectedParty),
                        recipientSponsoringEmployer.map(_.name)
                      )
                  }
                case IdentityType.Other =>
                  val recipientSponsoringEmployer =
                    request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
                  request.userAnswers
                    .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
                    .map(
                      other =>
                        (
                          other.name,
                          RecipientIdentityType(IdentityType.Other, None, None, Some(other.description)),
                          recipientSponsoringEmployer.contains(ConnectedParty),
                          recipientSponsoringEmployer.map(_.name)
                        )
                    )
              }

              for {
                recipientIdentityDetails <- optRecipientIdentityDetails
                (recipientName, recipientIdentityType, connectedParty, optRecipientSponsoringEmployer) = recipientIdentityDetails
                equalInstallments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index))
                datePeriodLoanDetails <- request.userAnswers.get(DatePeriodLoanPage(srn, index))
                loanAmountDetails <- request.userAnswers.get(AmountOfTheLoanPage(srn, index))
                (loanAmount, capRepaymentCY, amountOutstanding) = loanAmountDetails
                optSecurity <- request.userAnswers.get(SecurityGivenForLoanPage(srn, index)).map(_.value.toOption)
                loanInterestDetails <- request.userAnswers.get(InterestOnLoanPage(srn, index))
                (loanInterestAmount, loanInterestRate, intReceivedCY) = loanInterestDetails
                optOutstandingArrearsOnLoan <- request.userAnswers
                  .get(OutstandingArrearsOnLoanPage(srn, index))
                  .map(_.value.toOption)
              } yield {
                LoanTransactions(
                  recipientIdentityType,
                  recipientName,
                  connectedParty,
                  optRecipientSponsoringEmployer,
                  LoanPeriod(datePeriodLoanDetails._1, datePeriodLoanDetails._2.value, datePeriodLoanDetails._3),
                  LoanAmountDetails(
                    loanAmount.value,
                    capRepaymentCY.value,
                    amountOutstanding.value
                  ),
                  equalInstallments,
                  LoanInterestDetails(
                    loanInterestAmount.value,
                    loanInterestRate.value,
                    intReceivedCY.value
                  ),
                  optSecurity.map(_.security),
                  optOutstandingArrearsOnLoan.map(_.value)
                )
              }
          }
      }
      .toList
      .flatten
  }
}
