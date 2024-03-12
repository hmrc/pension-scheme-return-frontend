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

import cats.implicits.catsSyntaxTuple2Semigroupal
import com.google.inject.Singleton
import config.Refined.{Max5000, OneTo50, OneTo5000}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import models.HowSharesDisposed.{HowSharesDisposed, Other, Redeemed, Sold}
import models.IdentitySubject.SharesSeller
import models.SchemeHoldShare.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.TypeOfShares.{SponsoringEmployer, Unquoted}
import models.UserAnswers.implicits.UserAnswersTryOps
import models._
import models.requests.DataRequest
import models.requests.psr._
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.shares._
import pages.nonsipp.sharesdisposal._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import uk.gov.hmrc.domain.Nino
import viewmodels.models.SectionCompleted

import javax.inject.Inject
import scala.util.Try

@Singleton()
class SharesTransformer @Inject() extends Transformer {

  def transformToEtmp(
    srn: Srn,
    sharesDisposal: Boolean
  )(implicit request: DataRequest[_]): Shares =
    Shares(
      optShareTransactions = buildOptShareTransactions(srn, sharesDisposal),
      optTotalValueQuotedShares = buildOptQuotedShares(srn)
    )

  private def buildOptQuotedShares(srn: Srn)(implicit request: DataRequest[_]): Option[Double] = {
    val totalValueQuotedSharesPage = request.userAnswers.get(TotalValueQuotedSharesPage(srn))
    totalValueQuotedSharesPage.map(money => money.value)
  }

  private def buildOptShareTransactions(
    srn: Srn,
    sharesDisposal: Boolean
  )(implicit request: DataRequest[_]): Option[List[ShareTransaction]] =
    request.userAnswers
      .get(TypeOfSharesHeldPages(srn))
      .map { jsValue =>
        jsValue.keys.toList
          .flatMap { key =>
            key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
              case None => None
              case Some(index) =>
                for {
                  typeOfSharesHeld <- request.userAnswers.get(TypeOfSharesHeldPage(srn, index))
                  schemeHoldShare <- request.userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index))
                  nameOfSharesCompany <- request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index))
                  eitherSharesCompanyCrnOrReason <- request.userAnswers.get(SharesCompanyCrnPage(srn, index))
                  classOfShares <- request.userAnswers.get(ClassOfSharesPage(srn, index))
                  totalShares <- request.userAnswers.get(HowManySharesPage(srn, index))
                  costOfShares <- request.userAnswers.get(CostOfSharesPage(srn, index))
                  supportedByIndepValuation <- request.userAnswers.get(SharesIndependentValuationPage(srn, index))
                  totalDividendsOrReceipts <- request.userAnswers.get(SharesTotalIncomePage(srn, index))

                } yield {

                  val optDateOfAcqOrContrib = Option.when(schemeHoldShare != Transfer)(
                    request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).get
                  )

                  val optConnectedPartyStatus = Option.when(typeOfSharesHeld == Unquoted)(
                    request.userAnswers.get(SharesFromConnectedPartyPage(srn, index)).get
                  )

                  val optTotalAssetValue =
                    Option.when((schemeHoldShare == Acquisition) && (typeOfSharesHeld == SponsoringEmployer))(
                      request.userAnswers.get(TotalAssetValuePage(srn, index)).get
                    )

                  val optAcquisitionRelatedDetails = buildOptAcquisitionRelatedDetails(schemeHoldShare, srn, index)

                  ShareTransaction(
                    typeOfSharesHeld = typeOfSharesHeld,
                    shareIdentification = ShareIdentification(
                      nameOfSharesCompany = nameOfSharesCompany,
                      optCrnNumber = eitherSharesCompanyCrnOrReason.value.toOption.map(_.crn),
                      optReasonNoCRN = eitherSharesCompanyCrnOrReason.value.left.toOption,
                      classOfShares = classOfShares
                    ),
                    heldSharesTransaction = HeldSharesTransaction(
                      schemeHoldShare = schemeHoldShare,
                      optDateOfAcqOrContrib = optDateOfAcqOrContrib,
                      totalShares = totalShares,
                      optAcquiredFromName = optAcquisitionRelatedDetails.map(_._1),
                      optPropertyAcquiredFrom = optAcquisitionRelatedDetails.map(_._2),
                      optConnectedPartyStatus = optConnectedPartyStatus,
                      costOfShares = costOfShares.value,
                      supportedByIndepValuation = supportedByIndepValuation,
                      optTotalAssetValue = optTotalAssetValue.map(_.value),
                      totalDividendsOrReceipts = totalDividendsOrReceipts.value
                    ),
                    optDisposedSharesTransaction = Option
                      .when(sharesDisposal)(buildOptDisposedSharesTransactions(srn, index))
                  )
                }
            }
          }
      }

  private def buildOptAcquisitionRelatedDetails(
    schemeHoldShare: SchemeHoldShare,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(String, PropertyAcquiredFrom)] =
    Option
      .when(schemeHoldShare == Acquisition) {
        val identityType =
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)).get

        identityType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(IndividualNameOfSharesSellerPage(srn, index)),
              request.userAnswers.get(SharesIndividualSellerNINumberPage(srn, index)).map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyNameOfSharesSellerPage(srn, index)),
              request.userAnswers
                .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipShareSellerNamePage(srn, index)),
              request.userAnswers
                .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller))
              .map(
                other =>
                  (
                    other.name,
                    PropertyAcquiredFrom(
                      identityType = identityType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )

        }
      }
      .flatten

  private def buildOptDisposedSharesTransactions(
    srn: Srn,
    shareIndex: Refined[Int, OneTo5000]
  )(
    implicit request: DataRequest[_]
  ): Seq[DisposedSharesTransaction] =
    request.userAnswers
      .map(
        HowWereSharesDisposedPagesForShare(srn, shareIndex)
      )
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo50](i + 1).toOption) match {
          case None => None
          case Some(disposalIndex) =>
            for {
              howWereSharesDisposed <- request.userAnswers.get(
                HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)
              )
              totalSharesNowHeld <- request.userAnswers
                .get(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex))

            } yield {

              DisposedSharesTransaction(
                methodOfDisposal = howWereSharesDisposed.name,
                optOtherMethod = howWereSharesDisposed match {
                  case Other(details) => Some(details)
                  case _ => None
                },
                optSalesQuestions = buildOptSalesQuestions(howWereSharesDisposed, srn, shareIndex, disposalIndex),
                optRedemptionQuestions =
                  buildOptRedemptionQuestions(howWereSharesDisposed, srn, shareIndex, disposalIndex),
                totalSharesNowHeld = totalSharesNowHeld
              )
            }
        }
      }

  private def buildOptRedemptionQuestions(
    howWereSharesDisposedPage: HowSharesDisposed,
    srn: Srn,
    shareIndex: Refined[Int, OneTo5000],
    disposalIndex: Refined[Int, OneTo50]
  )(implicit request: DataRequest[_]): Option[RedemptionQuestions] =
    Option
      .when(howWereSharesDisposedPage == Redeemed) {
        val dateOfRedemption = request.userAnswers.get(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex)).get
        val noOfSharesRedeemed = request.userAnswers.get(HowManySharesRedeemedPage(srn, shareIndex, disposalIndex)).get
        val amountReceived =
          request.userAnswers.get(TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex)).get
        RedemptionQuestions(
          dateOfRedemption = dateOfRedemption,
          noOfSharesRedeemed = noOfSharesRedeemed,
          amountReceived = amountReceived.value
        )
      }

  private def buildOptSalesQuestions(
    howWereSharesDisposedPage: HowSharesDisposed,
    srn: Srn,
    shareIndex: Refined[Int, OneTo5000],
    disposalIndex: Refined[Int, OneTo50]
  )(implicit request: DataRequest[_]): Option[SalesQuestions] =
    Option
      .when(howWereSharesDisposedPage == Sold) {
        val dateOfSale = request.userAnswers.get(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex)).get
        val noOfSharesSold = request.userAnswers.get(HowManySharesSoldPage(srn, shareIndex, disposalIndex)).get
        val amountReceived =
          request.userAnswers.get(TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex)).get
        val whoWereTheSharesSoldTo =
          request.userAnswers.get(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex)).get
        val purchaserDetails = (whoWereTheSharesSoldTo match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, shareIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipBuyerNamePage(srn, shareIndex, disposalIndex)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, shareIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = whoWereTheSharesSoldTo,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex))
              .map(
                other =>
                  (
                    other.name,
                    PropertyAcquiredFrom(
                      identityType = whoWereTheSharesSoldTo,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )
        }).get
        val connectedPartyStatus =
          request.userAnswers.get(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex)).get
        val supportedByIndepValuation =
          request.userAnswers.get(IndependentValuationPage(srn, shareIndex, disposalIndex)).get

        SalesQuestions(
          dateOfSale = dateOfSale,
          noOfSharesSold = noOfSharesSold,
          amountReceived = amountReceived.value,
          nameOfPurchaser = purchaserDetails._1,
          purchaserType = purchaserDetails._2,
          connectedPartyStatus = connectedPartyStatus,
          supportedByIndepValuation = supportedByIndepValuation
        )
      }

  def transformFromEtmp(
    userAnswers: UserAnswers,
    srn: Srn,
    shares: Shares
  ): Try[UserAnswers] = {

    val userAnswersOfShares = userAnswers.set(DidSchemeHoldAnySharesPage(srn), shares.optShareTransactions.isDefined)
    val userAnswersWithQuotedShares = shares.optTotalValueQuotedShares.fold(userAnswersOfShares)(
      totalValueQuotedShares => userAnswersOfShares.set(TotalValueQuotedSharesPage(srn), Money(totalValueQuotedShares))
    )

    shares.optShareTransactions.fold(userAnswersWithQuotedShares) { shareTransactions =>
      for {
        indexes <- buildIndexesForMax5000(shareTransactions.size)
        resultUA <- indexes.foldLeft(userAnswersWithQuotedShares) {
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

            val triedUA = for {
              ua0 <- ua
              ua1 <- ua0.set(TypeOfSharesHeldPage(srn, index), typeOfSharesHeld)
              ua2 <- ua1.set(WhyDoesSchemeHoldSharesPage(srn, index), heldSharesTransaction.schemeHoldShare)
              ua3 <- optDateOfAcqOrContrib.map(t => ua2.set(t._1, t._2)).getOrElse(Try(ua2))
              ua4 <- ua3.set(CompanyNameRelatedSharesPage(srn, index), shareIdentification.nameOfSharesCompany)
              ua5 <- ua4.set(SharesCompanyCrnPage(srn, index), ukCompanyCrn)
              ua6 <- ua5.set(ClassOfSharesPage(srn, index), shareIdentification.classOfShares)
              ua7 <- ua6.set(HowManySharesPage(srn, index), heldSharesTransaction.totalShares)
              ua8 <- optIdentityType.map(t => ua7.set(t._1, t._2)).getOrElse(Try(ua7))
              ua9 <- optIndividualTuple.map(t => ua8.set(t._1._1, t._1._2)).getOrElse(Try(ua8))
              ua10 <- optIndividualTuple.map(t => ua9.set(t._2._1, t._2._2)).getOrElse(Try(ua9))
              ua11 <- optUKCompanyTuple.map(t => ua10.set(t._1._1, t._1._2)).getOrElse(Try(ua10))
              ua12 <- optUKCompanyTuple.map(t => ua11.set(t._2._1, t._2._2)).getOrElse(Try(ua11))
              ua13 <- optUKPartnershipTuple.map(t => ua12.set(t._1._1, t._1._2)).getOrElse(Try(ua12))
              ua14 <- optUKPartnershipTuple.map(t => ua13.set(t._2._1, t._2._2)).getOrElse(Try(ua13))
              ua15 <- optOther.map(t => ua14.set(t._1, t._2)).getOrElse(Try(ua14))
              ua16 <- optConnectedPartyStatus.map(t => ua15.set(t._1, t._2)).getOrElse(Try(ua15))
              ua17 <- ua16.set(CostOfSharesPage(srn, index), Money(heldSharesTransaction.costOfShares))
              ua18 <- ua17.set(
                SharesIndependentValuationPage(srn, index),
                heldSharesTransaction.supportedByIndepValuation
              )
              ua19 <- optTotalAssetValue.map(t => ua18.set(t._1, t._2)).getOrElse(Try(ua18))
              ua20 <- ua19.set(SharesTotalIncomePage(srn, index), Money(heldSharesTransaction.totalDividendsOrReceipts))
              ua21 <- ua20.set(SharesCompleted(srn, index), SectionCompleted)
            } yield {
              buildOptDisposedShareTransactionUA(
                index,
                srn,
                ua21,
                shareTransaction.optDisposedSharesTransaction
              )
            }
            triedUA.flatten
        }
      } yield resultUA
    }
  }

  private def buildOptDisposedShareTransactionUA(
    index: Max5000,
    srn: Srn,
    userAnswers: UserAnswers,
    optDisposedSharesTransaction: Option[Seq[DisposedSharesTransaction]]
  ): Try[UserAnswers] = {
    val initialUserAnswersOfDisposal = userAnswers.set(SharesDisposalPage(srn), optDisposedSharesTransaction.isDefined)

    optDisposedSharesTransaction
      .map(
        disposedSharesTransactions => {
          for {
            disposalIndexes <- buildIndexesForMax50(disposedSharesTransactions.size)
            resultDisposalUA <- disposalIndexes.foldLeft(initialUserAnswersOfDisposal) {
              case (disposalUA, disposalIndex) =>
                val disposedSharesTransaction = disposedSharesTransactions(disposalIndex.value - 1)
                val optRedemptionQuestions = disposedSharesTransaction.optRedemptionQuestions
                val optSalesQuestions = disposedSharesTransaction.optSalesQuestions

                val methodOfDisposal = disposedSharesTransaction.methodOfDisposal match {
                  case HowSharesDisposed.Sold.name => HowSharesDisposed.Sold
                  case HowSharesDisposed.Redeemed.name => HowSharesDisposed.Redeemed
                  case HowSharesDisposed.Transferred.name => HowSharesDisposed.Transferred
                  case HowSharesDisposed.Other.name =>
                    HowSharesDisposed.Other(disposedSharesTransaction.optOtherMethod.get)
                }

                val howWereSharesDisposed = HowWereSharesDisposedPage(srn, index, disposalIndex) -> methodOfDisposal
                val totalSharesNowHeld = HowManyDisposalSharesPage(srn, index, disposalIndex) -> disposedSharesTransaction.totalSharesNowHeld

                // Redemption Details
                val optDateOfRedemption = optRedemptionQuestions.map(
                  redemptionQuestions =>
                    WhenWereSharesRedeemedPage(srn, index, disposalIndex) -> redemptionQuestions.dateOfRedemption
                )
                val optNoOfSharesRedeemed = optRedemptionQuestions.map(
                  redemptionQuestions =>
                    HowManySharesRedeemedPage(srn, index, disposalIndex) -> redemptionQuestions.noOfSharesRedeemed
                )
                val optAmountReceivedRedeemed = optRedemptionQuestions.map(
                  redemptionQuestions =>
                    TotalConsiderationSharesRedeemedPage(srn, index, disposalIndex) -> Money(
                      redemptionQuestions.amountReceived
                    )
                )

                // Sale Details
                val optDateOfSale = optSalesQuestions.map(
                  salesQuestions => WhenWereSharesSoldPage(srn, index, disposalIndex) -> salesQuestions.dateOfSale
                )
                val optNoOfSharesSold = optSalesQuestions.map(
                  salesQuestions => {
                    HowManySharesSoldPage(srn, index, disposalIndex) -> salesQuestions.noOfSharesSold
                  }
                )
                val optAmountReceivedSold = optSalesQuestions.map(
                  salesQuestions => {
                    TotalConsiderationSharesSoldPage(srn, index, disposalIndex) -> Money(salesQuestions.amountReceived)
                  }
                )
                val optPurchaserTyped = optSalesQuestions.map(
                  salesQuestions => {
                    WhoWereTheSharesSoldToPage(srn, index, disposalIndex) -> salesQuestions.purchaserType.identityType
                  }
                )

                val optIndividualTuple = optSalesQuestions
                  .filter(salesQuestions => salesQuestions.purchaserType.identityType == IdentityType.Individual)
                  .map(salesQuestions => {
                    val name = SharesIndividualBuyerNamePage(srn, index, disposalIndex) -> salesQuestions.nameOfPurchaser
                    val yesNoValue = salesQuestions.purchaserType.idNumber
                      .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Nino](
                          salesQuestions.purchaserType.reasonNoIdNumber.get
                        )
                      )

                    (name, IndividualBuyerNinoNumberPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optUKCompanyTuple = optSalesQuestions
                  .filter(salesQuestions => salesQuestions.purchaserType.identityType == IdentityType.UKCompany)
                  .map(salesQuestions => {
                    val name = CompanyBuyerNamePage(srn, index, disposalIndex) -> salesQuestions.nameOfPurchaser
                    val yesNoValue = salesQuestions.purchaserType.idNumber
                      .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Crn](
                          salesQuestions.purchaserType.reasonNoIdNumber.get
                        )
                      )

                    (name, CompanyBuyerCrnPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optUKPartnershipTuple = optSalesQuestions
                  .filter(salesQuestions => salesQuestions.purchaserType.identityType == IdentityType.UKPartnership)
                  .map(salesQuestions => {
                    val name = PartnershipBuyerNamePage(srn, index, disposalIndex) -> salesQuestions.nameOfPurchaser
                    val yesNoValue = salesQuestions.purchaserType.idNumber
                      .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Utr](
                          salesQuestions.purchaserType.reasonNoIdNumber.get
                        )
                      )

                    (name, PartnershipBuyerUtrPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optOther = optSalesQuestions
                  .filter(salesQuestions => salesQuestions.purchaserType.identityType == IdentityType.Other)
                  .map(salesQuestions => {
                    OtherBuyerDetailsPage(srn, index, disposalIndex) -> RecipientDetails(
                      salesQuestions.nameOfPurchaser,
                      salesQuestions.purchaserType.otherDescription.get
                    )
                  })

                val optConnectedPartyStatus = optSalesQuestions.map(
                  salesQuestions => {
                    IsBuyerConnectedPartyPage(srn, index, disposalIndex) -> salesQuestions.connectedPartyStatus
                  }
                )
                val optSupportedByIndepValuation = optSalesQuestions.map(
                  salesQuestions => {
                    IndependentValuationPage(srn, index, disposalIndex) -> salesQuestions.supportedByIndepValuation
                  }
                )

                for {
                  disposalUA0 <- disposalUA
                  disposalUA1 <- disposalUA0
                    .set(SharesDisposalCompletedPage(srn, index, disposalIndex), SectionCompleted)
                  disposalUA2 <- disposalUA1.set(howWereSharesDisposed._1, howWereSharesDisposed._2)
                  disposalUA3 <- disposalUA2.set(totalSharesNowHeld._1, totalSharesNowHeld._2)
                  disposalUA4 <- optDateOfRedemption
                    .map(t => disposalUA3.set(t._1, t._2))
                    .getOrElse(Try(disposalUA3))
                  disposalUA5 <- optNoOfSharesRedeemed
                    .map(t => disposalUA4.set(t._1, t._2))
                    .getOrElse(Try(disposalUA4))
                  disposalUA6 <- optAmountReceivedRedeemed
                    .map(t => disposalUA5.set(t._1, t._2))
                    .getOrElse(Try(disposalUA5))
                  disposalUA7 <- optDateOfSale
                    .map(t => disposalUA6.set(t._1, t._2))
                    .getOrElse(Try(disposalUA6))
                  disposalUA8 <- optNoOfSharesSold
                    .map(t => disposalUA7.set(t._1, t._2))
                    .getOrElse(Try(disposalUA7))
                  disposalUA9 <- optAmountReceivedSold
                    .map(t => disposalUA8.set(t._1, t._2))
                    .getOrElse(Try(disposalUA8))
                  disposalUA10 <- optPurchaserTyped
                    .map(t => disposalUA9.set(t._1, t._2))
                    .getOrElse(Try(disposalUA9))

                  disposalUA11 <- optIndividualTuple
                    .map(t => disposalUA10.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA10))
                  disposalUA12 <- optIndividualTuple
                    .map(t => disposalUA11.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA11))
                  disposalUA13 <- optUKCompanyTuple
                    .map(t => disposalUA12.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA12))
                  disposalUA14 <- optUKCompanyTuple
                    .map(t => disposalUA13.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA13))
                  disposalUA15 <- optUKPartnershipTuple
                    .map(t => disposalUA14.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA14))
                  disposalUA16 <- optUKPartnershipTuple
                    .map(t => disposalUA15.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA15))
                  disposalUA17 <- optOther.map(t => disposalUA16.set(t._1, t._2)).getOrElse(Try(disposalUA16))
                  disposalUA18 <- optConnectedPartyStatus
                    .map(t => disposalUA17.set(t._1, t._2))
                    .getOrElse(Try(disposalUA17))
                  disposalUA19 <- optSupportedByIndepValuation
                    .map(t => disposalUA18.set(t._1, t._2))
                    .getOrElse(Try(disposalUA18))

                } yield disposalUA19
            }
          } yield resultDisposalUA
        }
      )
      .getOrElse(initialUserAnswersOfDisposal)
  }
}
