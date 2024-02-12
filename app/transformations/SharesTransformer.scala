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

import com.google.inject.Singleton
import models.IdentitySubject.SharesSeller
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr._
import models.{ConditionalYesNo, Crn, IdentitySubject, IdentityType, Money, RecipientDetails, UserAnswers, Utr}
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.shares._
import uk.gov.hmrc.domain.Nino
import viewmodels.models.SectionCompleted

import javax.inject.Inject
import scala.util.Try

@Singleton()
class SharesTransformer @Inject()(shareTransactionTransformer: ShareTransactionTransformer) extends Transformer {

  def transformToEtmp(srn: Srn)(implicit request: DataRequest[_]): Shares =
    Shares(
      optShareTransactions = shareTransactionTransformer.transformToEtmp(srn)
    )

  def transformFromEtmp(
    userAnswers: UserAnswers,
    srn: Srn,
    shares: Shares
  ): Try[UserAnswers] = {
    shares.optShareTransactions.fold(Try(userAnswers)) { shareTransactions =>
      for {
        indexes <- buildIndexesForMax5000(shareTransactions.size)
        resultUA <- indexes.foldLeft(Try(userAnswers)) {
          case (ua, index) =>
            val shareTransaction = shareTransactions(index.value - 1)

            val typeOfSharesHeld = shareTransaction.typeOfSharesHeld
            val shareIdentification = shareTransaction.shareIdentification
            val heldSharesTransaction = shareTransaction.heldSharesTransaction

            val optDateOfAcqOrContrib = heldSharesTransaction.optDateOfAcqOrContrib.map(
              dateOfAcqOrContrib => WhenDidSchemeAcquireSharesPage(srn, index) -> dateOfAcqOrContrib
            )

            val ukCompanyCrn = shareIdentification.optCrnNumber
              .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
              .getOrElse(
                ConditionalYesNo.no[String, Crn](
                  shareIdentification.optReasonNoCRN.get
                )
              )

            val optIdentityType = heldSharesTransaction.optPropertyAcquiredFrom.map(
              prop => {
                IdentityTypePage(srn, index, IdentitySubject.SharesSeller) -> prop.identityType
              }
            )

            val optIndividualTuple = heldSharesTransaction.optPropertyAcquiredFrom
              .filter(prop => prop.identityType == IdentityType.Individual)
              .map(
                prop => {
                  val name = IndividualNameOfSharesSellerPage(srn, index) -> heldSharesTransaction.optAcquiredFromName.get
                  val yesNoValue = prop.idNumber
                    .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                    .getOrElse(
                      ConditionalYesNo.no[String, Nino](
                        prop.reasonNoIdNumber.get
                      )
                    )

                  (name, SharesIndividualSellerNINumberPage(srn, index) -> yesNoValue)
                }
              )

            val optUKCompanyTuple = heldSharesTransaction.optPropertyAcquiredFrom
              .filter(prop => prop.identityType == IdentityType.UKCompany)
              .map(
                prop => {
                  val name = CompanyNameOfSharesSellerPage(srn, index) -> heldSharesTransaction.optAcquiredFromName.get
                  val yesNoValue = prop.idNumber
                    .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                    .getOrElse(
                      ConditionalYesNo.no[String, Crn](
                        prop.reasonNoIdNumber.get
                      )
                    )
                  (name, CompanyRecipientCrnPage(srn, index, SharesSeller) -> yesNoValue)
                }
              )

            val optUKPartnershipTuple = heldSharesTransaction.optPropertyAcquiredFrom
              .filter(prop => prop.identityType == IdentityType.UKPartnership)
              .map(
                prop => {
                  val name = PartnershipShareSellerNamePage(srn, index) -> heldSharesTransaction.optAcquiredFromName.get
                  val yesNoValue = prop.idNumber
                    .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                    .getOrElse(
                      ConditionalYesNo.no[String, Utr](
                        prop.reasonNoIdNumber.get
                      )
                    )

                  (name, PartnershipRecipientUtrPage(srn, index, SharesSeller) -> yesNoValue)
                }
              )

            val optOther = heldSharesTransaction.optPropertyAcquiredFrom
              .filter(prop => prop.identityType == IdentityType.Other)
              .map(
                prop => {
                  OtherRecipientDetailsPage(srn, index, SharesSeller) -> RecipientDetails(
                    heldSharesTransaction.optAcquiredFromName.get,
                    prop.otherDescription.get
                  )
                }
              )

            val optConnectedPartyStatus = heldSharesTransaction.optConnectedPartyStatus.map(
              connectedPartyStatus => SharesFromConnectedPartyPage(srn, index) -> connectedPartyStatus
            )

            val optTotalAssetValue = heldSharesTransaction.optTotalAssetValue.map(
              totalAssetValue => TotalAssetValuePage(srn, index) -> Money(totalAssetValue)
            )

            for {
              ua0 <- ua
              ua1 <- ua0.set(DidSchemeHoldAnySharesPage(srn), true)
              ua2 <- ua1.set(TypeOfSharesHeldPage(srn, index), typeOfSharesHeld)
              ua3 <- ua2.set(WhyDoesSchemeHoldSharesPage(srn, index), heldSharesTransaction.schemeHoldShare)
              ua4 <- optDateOfAcqOrContrib.map(t => ua3.set(t._1, t._2)).getOrElse(Try(ua3))
              ua5 <- ua4.set(CompanyNameRelatedSharesPage(srn, index), shareIdentification.nameOfSharesCompany)
              ua6 <- ua5.set(SharesCompanyCrnPage(srn, index), ukCompanyCrn)
              ua7 <- ua6.set(ClassOfSharesPage(srn, index), shareIdentification.classOfShares)
              ua8 <- ua7.set(HowManySharesPage(srn, index), heldSharesTransaction.totalShares)
              ua9 <- optIdentityType.map(t => ua8.set(t._1, t._2)).getOrElse(Try(ua8))
              ua10 <- optIndividualTuple.map(t => ua9.set(t._1._1, t._1._2)).getOrElse(Try(ua9))
              ua11 <- optIndividualTuple.map(t => ua10.set(t._2._1, t._2._2)).getOrElse(Try(ua10))
              ua12 <- optUKCompanyTuple.map(t => ua11.set(t._1._1, t._1._2)).getOrElse(Try(ua11))
              ua13 <- optUKCompanyTuple.map(t => ua12.set(t._2._1, t._2._2)).getOrElse(Try(ua12))
              ua14 <- optUKPartnershipTuple.map(t => ua13.set(t._1._1, t._1._2)).getOrElse(Try(ua13))
              ua15 <- optUKPartnershipTuple.map(t => ua14.set(t._2._1, t._2._2)).getOrElse(Try(ua14))
              ua16 <- optOther.map(t => ua15.set(t._1, t._2)).getOrElse(Try(ua15))
              ua17 <- optConnectedPartyStatus.map(t => ua16.set(t._1, t._2)).getOrElse(Try(ua16))
              ua18 <- ua17.set(CostOfSharesPage(srn, index), Money(heldSharesTransaction.costOfShares))
              ua19 <- ua18.set(
                SharesIndependentValuationPage(srn, index),
                heldSharesTransaction.supportedByIndepValuation
              )
              ua20 <- optTotalAssetValue.map(t => ua19.set(t._1, t._2)).getOrElse(Try(ua19))
              ua21 <- ua20.set(SharesTotalIncomePage(srn, index), Money(heldSharesTransaction.totalDividendsOrReceipts))
              ua22 <- ua21.set(SharesCompleted(srn, index), SectionCompleted)
            } yield ua22
        }
      } yield resultUA
    }
  }
}
