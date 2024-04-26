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

package transformations

import pages.nonsipp.otherassetsdisposal._
import config.Refined.{Max5000, OneTo50, OneTo5000}
import models.SchemeId.Srn
import cats.implicits.catsSyntaxTuple2Semigroupal
import eu.timepit.refined.refineV
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.common._
import models.IdentitySubject.OtherAssetSeller
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import models.requests.DataRequest
import eu.timepit.refined.api.Refined
import pages.nonsipp.otherassetsheld._
import models.HowDisposed.{HowDisposed, Other, Sold}
import com.google.inject.Singleton
import models.requests.psr.{OtherAssetDisposed, OtherAssetTransaction, OtherAssets}
import models.UserAnswers.implicits.UserAnswersTryOps
import models._
import models.SchemeHoldAsset.{Acquisition, Transfer}

import scala.util.Try

import java.time.LocalDate
import javax.inject.Inject

@Singleton()
class OtherAssetTransactionsTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, otherAssetDisposed: Boolean)(
    implicit request: DataRequest[_]
  ): List[OtherAssetTransaction] =
    request.userAnswers
      .map(OtherAssetsCompleted.all(srn))
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              assetDescription <- request.userAnswers.get(WhatIsOtherAssetPage(srn, index))
              methodOfHolding <- request.userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index))
              costOfAsset <- request.userAnswers.get(CostOfOtherAssetPage(srn, index))
              movableSchedule29A <- request.userAnswers.get(IsAssetTangibleMoveablePropertyPage(srn, index))
              totalIncomeOrReceipts <- request.userAnswers.get(IncomeFromAssetPage(srn, index))
            } yield {

              val optNoneTransferRelatedDetails = buildOptNoneTransferRelatedDetails(methodOfHolding, srn, index)
              val optAcquisitionRelatedDetails = buildOptAcquisitionRelatedDetails(methodOfHolding, srn, index)

              OtherAssetTransaction(
                assetDescription = assetDescription,
                methodOfHolding = methodOfHolding,
                optDateOfAcqOrContrib = optNoneTransferRelatedDetails.map(_._2),
                costOfAsset = costOfAsset.value,
                optPropertyAcquiredFromName = optAcquisitionRelatedDetails.map(_._1),
                optPropertyAcquiredFrom = optAcquisitionRelatedDetails.map(_._3),
                optConnectedStatus = optAcquisitionRelatedDetails.map(_._2),
                optIndepValuationSupport = optNoneTransferRelatedDetails.map(_._1),
                movableSchedule29A = movableSchedule29A,
                totalIncomeOrReceipts = totalIncomeOrReceipts.value,
                optOtherAssetDisposed = Option.when(otherAssetDisposed)(buildOptOtherAssetsDisposed(srn, index))
              )
            }
        }
      }

  private def buildOptAcquisitionRelatedDetails(
    methodOfHolding: SchemeHoldAsset,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(String, Boolean, PropertyAcquiredFrom)] =
    Option
      .when(methodOfHolding == Acquisition) {
        val sellerIdentityType =
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller)).get
        val connectedPartyStatus =
          request.userAnswers.get(OtherAssetSellerConnectedPartyPage(srn, index)).get

        sellerIdentityType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(IndividualNameOfOtherAssetSellerPage(srn, index)),
              request.userAnswers.get(OtherAssetIndividualSellerNINumberPage(srn, index)).map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyNameOfOtherAssetSellerPage(srn, index)),
              request.userAnswers
                .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.OtherAssetSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipOtherAssetSellerNamePage(srn, index)),
              request.userAnswers
                .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.OtherAssetSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  connectedPartyStatus,
                  PropertyAcquiredFrom(
                    identityType = sellerIdentityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller))
              .map(
                other =>
                  (
                    other.name,
                    connectedPartyStatus,
                    PropertyAcquiredFrom(
                      identityType = sellerIdentityType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )

        }
      }
      .flatten

  private def buildOptNoneTransferRelatedDetails(
    methodOfHolding: SchemeHoldAsset,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(Boolean, LocalDate)] = Option.when(methodOfHolding != Transfer) {

    val indepValuationSupport = request.userAnswers.get(IndependentValuationPage(srn, index)).get
    val dateOfAcqOrContrib =
      request.userAnswers.get(WhenDidSchemeAcquireAssetsPage(srn, index)).get
    (
      indepValuationSupport,
      dateOfAcqOrContrib
    )
  }

  private def buildOptOtherAssetsDisposed(
    srn: Srn,
    otherAssetIndex: Refined[Int, OneTo5000]
  )(
    implicit request: DataRequest[_]
  ): Seq[OtherAssetDisposed] =
    request.userAnswers
      .map(
        HowWasAssetDisposedOfPagesForEachAsset(srn, otherAssetIndex)
      )
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo50](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              howWasAssetDisposed <- request.userAnswers.get(
                HowWasAssetDisposedOfPage(srn, otherAssetIndex, index)
              )
              anyPartAssetStillHeld <- request.userAnswers.get(
                AnyPartAssetStillHeldPage(srn, otherAssetIndex, index)
              )

            } yield {

              val optPurchaseRelatedDetails =
                buildOptPurchaseRelatedDetails(howWasAssetDisposed, srn, otherAssetIndex, index)

              OtherAssetDisposed(
                methodOfDisposal = howWasAssetDisposed.name,
                optOtherMethod = howWasAssetDisposed match {
                  case Other(details) => Some(details)
                  case _ => None
                },
                optDateSold = Option.when(howWasAssetDisposed == Sold)(
                  request.userAnswers
                    .get(
                      WhenWasAssetSoldPage(srn, otherAssetIndex, index)
                    )
                    .get
                ),
                optPurchaserName = optPurchaseRelatedDetails.map(_._1),
                optPropertyAcquiredFrom = optPurchaseRelatedDetails.map(_._2),
                optTotalAmountReceived = Option.when(howWasAssetDisposed == Sold)(
                  request.userAnswers
                    .get(
                      TotalConsiderationSaleAssetPage(srn, otherAssetIndex, index)
                    )
                    .get
                    .value
                ),
                optConnectedStatus = Option.when(howWasAssetDisposed == Sold)(
                  request.userAnswers
                    .get(
                      IsBuyerConnectedPartyPage(srn, otherAssetIndex, index)
                    )
                    .get
                ),
                optSupportedByIndepValuation = Option.when(howWasAssetDisposed == Sold)(
                  request.userAnswers
                    .get(
                      AssetSaleIndependentValuationPage(srn, otherAssetIndex, index)
                    )
                    .get
                ),
                anyPartAssetStillHeld = anyPartAssetStillHeld
              )
            }
        }
      }

  private def buildOptPurchaseRelatedDetails(
    howWasAssetDisposed: HowDisposed,
    srn: Srn,
    otherAssetIndex: Refined[Int, OneTo5000],
    disposalIndex: Refined[Int, OneTo50]
  )(implicit request: DataRequest[_]): Option[(String, PropertyAcquiredFrom)] = {

    Option
      .when(howWasAssetDisposed == Sold) {
        val purchaserType = request.userAnswers.get(TypeOfAssetBuyerPage(srn, otherAssetIndex, disposalIndex)).get
        purchaserType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(IndividualNameOfAssetBuyerPage(srn, otherAssetIndex, disposalIndex)),
              request.userAnswers
                .get(AssetIndividualBuyerNiNumberPage(srn, otherAssetIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyNameOfAssetBuyerPage(srn, otherAssetIndex, disposalIndex)),
              request.userAnswers
                .get(AssetCompanyBuyerCrnPage(srn, otherAssetIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipBuyerNamePage(srn, otherAssetIndex, disposalIndex)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, otherAssetIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = purchaserType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherBuyerDetailsPage(srn, otherAssetIndex, disposalIndex))
              .map(
                other =>
                  (
                    other.name,
                    PropertyAcquiredFrom(
                      identityType = purchaserType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )
        }
      }
      .flatten
  }

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, otherAssets: OtherAssets): Try[UserAnswers] = {
    val otherAssetTransactions = otherAssets.otherAssetTransactions
    for {
      indexes <- buildIndexesForMax5000(otherAssetTransactions.size)
      resultUA <- indexes.foldLeft(userAnswers.set(OtherAssetsHeldPage(srn), otherAssets.otherAssetsWereHeld)) {
        case (ua, index) =>
          val otherAssetTransaction = otherAssetTransactions(index.value - 1)

          val assetDescription = WhatIsOtherAssetPage(srn, index) -> otherAssetTransaction.assetDescription
          val methodOfHolding = WhyDoesSchemeHoldAssetsPage(srn, index) -> otherAssetTransaction.methodOfHolding
          val costOfAsset = CostOfOtherAssetPage(srn, index) -> Money(otherAssetTransaction.costOfAsset)
          val movableSchedule29A = IsAssetTangibleMoveablePropertyPage(srn, index) -> otherAssetTransaction.movableSchedule29A
          val totalIncomeOrReceipts = IncomeFromAssetPage(srn, index) -> Money(
            otherAssetTransaction.totalIncomeOrReceipts
          )

          val optDateOfAcqOrContrib = otherAssetTransaction.optDateOfAcqOrContrib.map(
            date => WhenDidSchemeAcquireAssetsPage(srn, index) -> date
          )
          val optIndepValuationSupport = otherAssetTransaction.optIndepValuationSupport.map(
            indepValuationSupport => IndependentValuationPage(srn, index) -> indepValuationSupport
          )
          val optConnectedStatus = otherAssetTransaction.optConnectedStatus.map(
            connectedStatus => OtherAssetSellerConnectedPartyPage(srn, index) -> connectedStatus
          )

          val optIdentityType = otherAssetTransaction.optPropertyAcquiredFrom.map(
            prop => {
              IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller) -> prop.identityType
            }
          )

          val optIndividualTuple = otherAssetTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Individual)
            .map(
              prop => {
                val name = IndividualNameOfOtherAssetSellerPage(srn, index) -> otherAssetTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Nino](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, OtherAssetIndividualSellerNINumberPage(srn, index) -> yesNoValue)
              }
            )

          val optUKCompanyTuple = otherAssetTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKCompany)
            .map(
              prop => {
                val name = CompanyNameOfOtherAssetSellerPage(srn, index) -> otherAssetTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Crn](
                      prop.reasonNoIdNumber.get
                    )
                  )
                (name, CompanyRecipientCrnPage(srn, index, OtherAssetSeller) -> yesNoValue)
              }
            )

          val optUKPartnershipTuple = otherAssetTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKPartnership)
            .map(
              prop => {
                val name = PartnershipOtherAssetSellerNamePage(srn, index) -> otherAssetTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Utr](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, PartnershipRecipientUtrPage(srn, index, OtherAssetSeller) -> yesNoValue)
              }
            )

          val optOther = otherAssetTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Other)
            .map(
              prop => {
                OtherRecipientDetailsPage(srn, index, OtherAssetSeller) -> RecipientDetails(
                  otherAssetTransaction.optPropertyAcquiredFromName.get,
                  prop.otherDescription.get
                )
              }
            )

          val otherAssetsCompleted = OtherAssetsCompleted(srn, index) -> SectionCompleted

          val triedUA = for {
            ua0 <- ua
            ua1 <- ua0.set(assetDescription._1, assetDescription._2)
            ua2 <- ua1.set(methodOfHolding._1, methodOfHolding._2)
            ua3 <- ua2.set(costOfAsset._1, costOfAsset._2)
            ua4 <- ua3.set(movableSchedule29A._1, movableSchedule29A._2)
            ua5 <- ua4.set(totalIncomeOrReceipts._1, totalIncomeOrReceipts._2)
            ua6 <- optDateOfAcqOrContrib.map(t => ua5.set(t._1, t._2)).getOrElse(Try(ua5))
            ua7 <- optIndepValuationSupport.map(t => ua6.set(t._1, t._2)).getOrElse(Try(ua6))
            ua8 <- optConnectedStatus.map(t => ua7.set(t._1, t._2)).getOrElse(Try(ua7))
            ua9 <- optIdentityType.map(t => ua8.set(t._1, t._2)).getOrElse(Try(ua8))
            ua10 <- optIndividualTuple.map(t => ua9.set(t._1._1, t._1._2)).getOrElse(Try(ua9))
            ua11 <- optIndividualTuple.map(t => ua10.set(t._2._1, t._2._2)).getOrElse(Try(ua10))
            ua12 <- optUKCompanyTuple.map(t => ua11.set(t._1._1, t._1._2)).getOrElse(Try(ua11))
            ua13 <- optUKCompanyTuple.map(t => ua12.set(t._2._1, t._2._2)).getOrElse(Try(ua12))
            ua14 <- optUKPartnershipTuple.map(t => ua13.set(t._1._1, t._1._2)).getOrElse(Try(ua13))
            ua15 <- optUKPartnershipTuple.map(t => ua14.set(t._2._1, t._2._2)).getOrElse(Try(ua14))
            ua16 <- optOther.map(t => ua15.set(t._1, t._2)).getOrElse(Try(ua15))
            ua17 <- ua16.set(otherAssetsCompleted._1, otherAssetsCompleted._2)
          } yield {
            buildOptDisposedOtherAssetsUA(
              index,
              srn,
              ua17,
              otherAssetTransaction.optOtherAssetDisposed
            )
          }
          triedUA.flatten
      }
    } yield resultUA
  }

  private def buildOptDisposedOtherAssetsUA(
    index: Max5000,
    srn: Srn,
    userAnswers: UserAnswers,
    optOtherAssetDisposed: Option[Seq[OtherAssetDisposed]]
  ): Try[UserAnswers] = {
    val initialUserAnswersOfDisposal = userAnswers.set(OtherAssetsDisposalPage(srn), optOtherAssetDisposed.isDefined)

    optOtherAssetDisposed
      .map(
        otherAssetDisposedList => {
          for {
            disposalIndexes <- buildIndexesForMax50(otherAssetDisposedList.size)
            resultDisposalUA <- disposalIndexes.foldLeft(initialUserAnswersOfDisposal) {
              case (disposalUA, disposalIndex) =>
                val otherAssetDisposed = otherAssetDisposedList(disposalIndex.value - 1)

                val methodOfDisposal = otherAssetDisposed.methodOfDisposal match {
                  case HowDisposed.Sold.name => HowDisposed.Sold
                  case HowDisposed.Transferred.name => HowDisposed.Transferred
                  case HowDisposed.Other.name =>
                    HowDisposed.Other(otherAssetDisposed.optOtherMethod.get)
                }

                val howWasAssetDisposed = HowWasAssetDisposedOfPage(srn, index, disposalIndex) -> methodOfDisposal
                val anyPartAssetStillHeld = AnyPartAssetStillHeldPage(srn, index, disposalIndex) -> otherAssetDisposed.anyPartAssetStillHeld

                val optDateSold = otherAssetDisposed.optDateSold.map(
                  dateSold => WhenWasAssetSoldPage(srn, index, disposalIndex) -> dateSold
                )
                val optTotalAmountReceived = otherAssetDisposed.optTotalAmountReceived.map(
                  totalAmountReceived =>
                    TotalConsiderationSaleAssetPage(srn, index, disposalIndex) -> Money(totalAmountReceived)
                )

                val optConnectedStatus = otherAssetDisposed.optConnectedStatus.map(
                  connectedStatus => {
                    IsBuyerConnectedPartyPage(srn, index, disposalIndex) -> connectedStatus
                  }
                )
                val optSupportedByIndepValuation = otherAssetDisposed.optSupportedByIndepValuation.map(
                  supportedByIndepValuation => {
                    AssetSaleIndependentValuationPage(srn, index, disposalIndex) -> supportedByIndepValuation
                  }
                )

                val optPurchaserTyped = otherAssetDisposed.optPropertyAcquiredFrom.map(
                  propertyAcquiredFrom => {
                    TypeOfAssetBuyerPage(srn, index, disposalIndex) -> propertyAcquiredFrom.identityType
                  }
                )

                val optIndividualTuple = otherAssetDisposed.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.Individual)
                  .map(prop => {
                    val name = IndividualNameOfAssetBuyerPage(srn, index, disposalIndex) -> otherAssetDisposed.optPurchaserName.get
                    val yesNoValue = prop.idNumber
                      .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Nino](
                          prop.reasonNoIdNumber.get
                        )
                      )

                    (name, AssetIndividualBuyerNiNumberPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optUKCompanyTuple = otherAssetDisposed.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.UKCompany)
                  .map(prop => {
                    val name = CompanyNameOfAssetBuyerPage(srn, index, disposalIndex) -> otherAssetDisposed.optPurchaserName.get
                    val yesNoValue = prop.idNumber
                      .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Crn](
                          prop.reasonNoIdNumber.get
                        )
                      )

                    (name, AssetCompanyBuyerCrnPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optUKPartnershipTuple = otherAssetDisposed.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.UKPartnership)
                  .map(prop => {
                    val name = PartnershipBuyerNamePage(srn, index, disposalIndex) -> otherAssetDisposed.optPurchaserName.get
                    val yesNoValue = prop.idNumber
                      .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                      .getOrElse(
                        ConditionalYesNo.no[String, Utr](
                          prop.reasonNoIdNumber.get
                        )
                      )

                    (name, PartnershipBuyerUtrPage(srn, index, disposalIndex) -> yesNoValue)
                  })

                val optOther = otherAssetDisposed.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.Other)
                  .map(prop => {
                    OtherBuyerDetailsPage(srn, index, disposalIndex) -> RecipientDetails(
                      otherAssetDisposed.optPurchaserName.get,
                      prop.otherDescription.get
                    )
                  })

                for {
                  disposalUA0 <- disposalUA
                  disposalUA1 <- disposalUA0
                    .set(OtherAssetsDisposalProgress(srn, index, disposalIndex), SectionJourneyStatus.Completed)
                    .set(OtherAssetsDisposalCompleted(srn), SectionCompleted)
                  disposalUA2 <- disposalUA1.set(howWasAssetDisposed._1, howWasAssetDisposed._2)
                  disposalUA3 <- disposalUA2.set(anyPartAssetStillHeld._1, anyPartAssetStillHeld._2)
                  disposalUA4 <- optPurchaserTyped
                    .map(t => disposalUA3.set(t._1, t._2))
                    .getOrElse(Try(disposalUA3))
                  disposalUA5 <- optDateSold
                    .map(t => disposalUA4.set(t._1, t._2))
                    .getOrElse(Try(disposalUA4))
                  disposalUA6 <- optTotalAmountReceived
                    .map(t => disposalUA5.set(t._1, t._2))
                    .getOrElse(Try(disposalUA5))
                  disposalUA7 <- optConnectedStatus
                    .map(t => disposalUA6.set(t._1, t._2))
                    .getOrElse(Try(disposalUA6))
                  disposalUA8 <- optSupportedByIndepValuation
                    .map(t => disposalUA7.set(t._1, t._2))
                    .getOrElse(Try(disposalUA7))
                  disposalUA9 <- optIndividualTuple
                    .map(t => disposalUA8.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA8))
                  disposalUA10 <- optIndividualTuple
                    .map(t => disposalUA9.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA9))
                  disposalUA11 <- optUKCompanyTuple
                    .map(t => disposalUA10.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA10))
                  disposalUA12 <- optUKCompanyTuple
                    .map(t => disposalUA11.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA11))
                  disposalUA13 <- optUKPartnershipTuple
                    .map(t => disposalUA12.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA12))
                  disposalUA14 <- optUKPartnershipTuple
                    .map(t => disposalUA13.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA13))
                  disposalUA15 <- optOther.map(t => disposalUA14.set(t._1, t._2)).getOrElse(Try(disposalUA14))

                } yield disposalUA15
            }
          } yield resultDisposalUA
        }
      )
      .getOrElse(initialUserAnswersOfDisposal)
  }
}
