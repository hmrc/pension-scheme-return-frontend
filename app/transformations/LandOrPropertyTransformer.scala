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

import models.SchemeHoldLandProperty.{Acquisition, Transfer}
import config.RefinedTypes.{Max5000, OneTo50, OneTo5000}
import pages.nonsipp.landorproperty._
import cats.implicits.catsSyntaxTuple2Semigroupal
import eu.timepit.refined.refineV
import uk.gov.hmrc.domain.Nino
import models._
import pages.nonsipp.common._
import models.IdentitySubject.LandOrPropertySeller
import viewmodels.models.SectionCompleted
import models.requests.DataRequest
import eu.timepit.refined.api.Refined
import models.HowDisposed.{HowDisposed, Other, Sold}
import com.google.inject.Singleton
import pages.nonsipp.landorproperty.Paths.landOrProperty
import models.SchemeId.Srn
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import models.requests.psr._
import pages.nonsipp.landorpropertydisposal._
import models.UserAnswers.implicits.UserAnswersTryOps

import scala.util.Try

import java.time.LocalDate
import javax.inject.Inject

@Singleton()
class LandOrPropertyTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, optLandOrPropertyHeld: Option[Boolean], initialUA: UserAnswers)(
    implicit request: DataRequest[_]
  ): Option[LandOrProperty] =
    Option.when(optLandOrPropertyHeld.nonEmpty || request.userAnswers.map(LandPropertyInUKPages(srn)).toList.nonEmpty) {
      val optDisposeAnyLandOrProperty = request.userAnswers.get(LandOrPropertyDisposalPage(srn))
      val dispose =
        if (isPrePopulation && optDisposeAnyLandOrProperty.isEmpty) {
          None // allow None only in pre-population
        } else {
          Option(optDisposeAnyLandOrProperty.getOrElse(false))
        }
      LandOrProperty(
        recordVersion = Option
          .when(request.userAnswers.get(landOrProperty) == initialUA.get(landOrProperty))(
            request.userAnswers.get(LandOrPropertyRecordVersionPage(srn))
          )
          .flatten,
        optLandOrPropertyHeld = optLandOrPropertyHeld,
        optDisposeAnyLandOrProperty = dispose,
        landOrPropertyTransactions = transformLandOrPropertyTransactionsToEtmp(srn, optDisposeAnyLandOrProperty)
      )
    }

  private def transformLandOrPropertyTransactionsToEtmp(srn: Srn, optDisposeAnyLandOrProperty: Option[Boolean])(
    implicit request: DataRequest[_]
  ): List[LandOrPropertyTransactions] =
    request.userAnswers
      .map(LandPropertyInUKPages(srn))
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              landOrPropertyInUK <- request.userAnswers.get(LandPropertyInUKPage(srn, index))
              addressDetails <- request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index))
              landRegistryTitleNumber <- request.userAnswers.get(LandRegistryTitleNumberPage(srn, index))
              methodOfHolding <- request.userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, index))

              totalCostOfLandOrProperty <- request.userAnswers.get(LandOrPropertyTotalCostPage(srn, index))
              prePopulated = request.userAnswers.get(LandOrPropertyPrePopulated(srn, index))
            } yield {

              val optNoneTransferRelatedDetails = buildOptNoneTransferRelatedDetails(methodOfHolding, srn, index)
              val optAcquisitionRelatedDetails = buildOptAcquisitionRelatedDetails(methodOfHolding, srn, index)

              LandOrPropertyTransactions(
                prePopulated = prePopulated,
                propertyDetails = PropertyDetails(
                  landOrPropertyInUK = landOrPropertyInUK,
                  addressDetails = addressDetails,
                  landRegistryTitleNumberKey = landRegistryTitleNumber.value.isRight,
                  landRegistryTitleNumberValue = landRegistryTitleNumber.value.merge
                ),
                heldPropertyTransaction = HeldPropertyTransaction(
                  methodOfHolding = methodOfHolding,
                  dateOfAcquisitionOrContribution = optNoneTransferRelatedDetails.map(_._2),
                  optPropertyAcquiredFromName = optAcquisitionRelatedDetails.map(_._1),
                  optPropertyAcquiredFrom = optAcquisitionRelatedDetails.map(_._3),
                  optConnectedPartyStatus = optAcquisitionRelatedDetails.map(_._2),
                  totalCostOfLandOrProperty = totalCostOfLandOrProperty.value,
                  optIndepValuationSupport = optNoneTransferRelatedDetails.map(_._1),
                  optIsLandOrPropertyResidential = request.userAnswers.get(IsLandOrPropertyResidentialPage(srn, index)),
                  optLeaseDetails = buildOptLandOrPropertyLeasedDetails(
                    request.userAnswers.get(IsLandPropertyLeasedPage(srn, index)).getOrElse(false),
                    srn,
                    index
                  ),
                  optLandOrPropertyLeased = request.userAnswers.get(IsLandPropertyLeasedPage(srn, index)),
                  optTotalIncomeOrReceipts = request.userAnswers
                    .get(LandOrPropertyTotalIncomePage(srn, index))
                    .fold(None: Option[Double])(
                      money => Some(money.value)
                    )
                ),
                optDisposedPropertyTransaction = Option
                  .when(optDisposeAnyLandOrProperty.getOrElse(false))(buildOptDisposedPropertyTransactions(srn, index))
              )
            }
        }
      }

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, landOrProperty: LandOrProperty): Try[UserAnswers] = {
    val landOrPropertyTransactions = landOrProperty.landOrPropertyTransactions
    val userAnswersOfLandOrPropertyHeld = landOrProperty.optLandOrPropertyHeld match {
      case Some(value) => userAnswers.set(LandOrPropertyHeldPage(srn), value)
      case None => Try(userAnswers)
    }
    val userAnswersWithRecordVersion =
      landOrProperty.recordVersion.fold(userAnswersOfLandOrPropertyHeld)(
        userAnswersOfLandOrPropertyHeld.set(LandOrPropertyRecordVersionPage(srn), _)
      )

    for {
      indexes <- buildIndexesForMax5000(landOrPropertyTransactions.size)
      resultUA <- indexes.foldLeft(userAnswersWithRecordVersion) {
        case (ua, index) =>
          val propertyDetails = landOrPropertyTransactions(index.value - 1).propertyDetails
          val heldPropertyTransaction = landOrPropertyTransactions(index.value - 1).heldPropertyTransaction

          val landOrPropertyInUK = LandPropertyInUKPage(srn, index) -> propertyDetails.landOrPropertyInUK
          val addressDetails = LandOrPropertyChosenAddressPage(srn, index) -> propertyDetails.addressDetails

          val landRegistryTitleNumber = buildLandRegistryTitleNumberDetail(srn, index, propertyDetails)
          val methodOfHolding = WhyDoesSchemeHoldLandPropertyPage(srn, index) -> heldPropertyTransaction.methodOfHolding
          val totalCostOfLandOrProperty = LandOrPropertyTotalCostPage(srn, index) -> Money(
            heldPropertyTransaction.totalCostOfLandOrProperty
          )
          val optIsLandOrPropertyResidential = heldPropertyTransaction.optIsLandOrPropertyResidential.map(
            isResidential => IsLandOrPropertyResidentialPage(srn, index) -> isResidential
          )
          val optLandOrPropertyLeased = heldPropertyTransaction.optLandOrPropertyLeased.map(
            isLandOrPropertyLeased => IsLandPropertyLeasedPage(srn, index) -> isLandOrPropertyLeased
          )
          val optTotalIncomeOrReceipts = heldPropertyTransaction.optTotalIncomeOrReceipts.map(
            totalIncomeOrReceipts => LandOrPropertyTotalIncomePage(srn, index) -> Money(totalIncomeOrReceipts)
          )
          val optDateOfAcquisitionOrContribution = heldPropertyTransaction.dateOfAcquisitionOrContribution.map(
            date => LandOrPropertyWhenDidSchemeAcquirePage(srn, index) -> date
          )
          val optIndepValuationSupport = heldPropertyTransaction.optIndepValuationSupport.map(
            indepValuationSupport => LandPropertyIndependentValuationPage(srn, index) -> indepValuationSupport
          )
          val optSellerConnectedParty = heldPropertyTransaction.optConnectedPartyStatus.map(
            status => LandOrPropertySellerConnectedPartyPage(srn, index) -> status
          )
          val optReceivedLandType = heldPropertyTransaction.optPropertyAcquiredFrom.map(
            prop => {
              IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller) -> prop.identityType
            }
          )
          val optIndividualTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Individual)
            .map(
              prop => {
                val name = LandPropertyIndividualSellersNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Nino](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, IndividualSellerNiPage(srn, index) -> yesNoValue)
              }
            )

          val optUKCompanyTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKCompany)
            .map(
              prop => {
                val name = CompanySellerNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Crn](
                      prop.reasonNoIdNumber.get
                    )
                  )
                (name, CompanyRecipientCrnPage(srn, index, LandOrPropertySeller) -> yesNoValue)
              }
            )

          val optUKPartnershipTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKPartnership)
            .map(
              prop => {
                val name = PartnershipSellerNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Utr](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, PartnershipRecipientUtrPage(srn, index, LandOrPropertySeller) -> yesNoValue)
              }
            )

          val optOther = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Other)
            .map(
              prop => {
                OtherRecipientDetailsPage(srn, index, LandOrPropertySeller) -> RecipientDetails(
                  heldPropertyTransaction.optPropertyAcquiredFromName.get,
                  prop.otherDescription.get
                )
              }
            )

          val optLeaseTuple = heldPropertyTransaction.optLeaseDetails.map(
            leaseDetails => {
              val ldp = LandOrPropertyLeaseDetailsPage(srn, index) ->
                leaseDetails.optLesseeName.map(
                  lesseeName =>
                    (
                      lesseeName,
                      Money(leaseDetails.optAnnualLeaseAmount.get.doubleValue), // assume it is present when name is present
                      leaseDetails.optLeaseGrantDate.get
                    )
                )
              val leaseConnectedParty = IsLesseeConnectedPartyPage(srn, index) ->
                leaseDetails.optConnectedPartyStatus

              (ldp, leaseConnectedParty)
            }
          )

          val landOrPropertyCompleted = LandOrPropertyCompleted(srn, index) -> SectionCompleted

          val landOrPropertyPrePopulated = indexes
            .filter(
              index => {
                landOrPropertyTransactions(index.value - 1).prePopulated.nonEmpty
              }
            )
            .map(
              index =>
                LandOrPropertyPrePopulated(srn, index) -> landOrPropertyTransactions(index.value - 1).prePopulated.get
            )

          val triedUA = for {
            ua0 <- ua
            ua1 <- ua0.set(landOrPropertyInUK._1, landOrPropertyInUK._2)
            ua2 <- ua1.set(addressDetails._1, addressDetails._2)
            ua3 <- ua2.set(landRegistryTitleNumber._1, landRegistryTitleNumber._2)
            ua4 <- ua3.set(methodOfHolding._1, methodOfHolding._2)
            ua5 <- ua4.set(totalCostOfLandOrProperty._1, totalCostOfLandOrProperty._2)
            ua6 <- optIsLandOrPropertyResidential.map(t => ua5.set(t._1, t._2)).getOrElse(Try(ua5))
            ua7 <- optLandOrPropertyLeased.map(t => ua6.set(t._1, t._2)).getOrElse(Try(ua6))
            ua8 <- optTotalIncomeOrReceipts.map(t => ua7.set(t._1, t._2)).getOrElse(Try(ua7))
            ua9 <- optDateOfAcquisitionOrContribution.map(t => ua8.set(t._1, t._2)).getOrElse(Try(ua8))
            ua10 <- optIndepValuationSupport.map(t => ua9.set(t._1, t._2)).getOrElse(Try(ua9))
            ua11 <- optReceivedLandType.map(t => ua10.set(t._1, t._2)).getOrElse(Try(ua10))

            ua12 <- optIndividualTuple.map(t => ua11.set(t._1._1, t._1._2)).getOrElse(Try(ua11))
            ua13 <- optIndividualTuple.map(t => ua12.set(t._2._1, t._2._2)).getOrElse(Try(ua12))
            ua14 <- optUKCompanyTuple.map(t => ua13.set(t._1._1, t._1._2)).getOrElse(Try(ua13))
            ua15 <- optUKCompanyTuple.map(t => ua14.set(t._2._1, t._2._2)).getOrElse(Try(ua14))
            ua16 <- optUKPartnershipTuple.map(t => ua15.set(t._1._1, t._1._2)).getOrElse(Try(ua15))
            ua17 <- optUKPartnershipTuple.map(t => ua16.set(t._2._1, t._2._2)).getOrElse(Try(ua16))
            ua18 <- optOther.map(t => ua17.set(t._1, t._2)).getOrElse(Try(ua17))

            ua19 <- optLeaseTuple
              .map(
                optLeaseDetails =>
                  optLeaseDetails._1._2.map(values => ua18.set(optLeaseDetails._1._1, values)).getOrElse(Try(ua18))
              )
              .getOrElse(Try(ua18))

            ua20 <- optLeaseTuple
              .map(
                optLesseeConnectedParty =>
                  optLesseeConnectedParty._2._2
                    .map(b => ua19.set(optLesseeConnectedParty._2._1, b))
                    .getOrElse(Try(ua19))
              )
              .getOrElse(Try(ua18))

            ua21 <- optSellerConnectedParty.map(t => ua20.set(t._1, t._2)).getOrElse(Try(ua20))
            ua22 <- ua21.set(landOrPropertyCompleted._1, landOrPropertyCompleted._2)
            ua23 <- landOrPropertyPrePopulated.foldLeft(Try(ua22)) {
              case (ua, (page, value)) => ua.flatMap(_.set(page, value))
            }
          } yield {
            buildOptDisposedTransactionUA(
              index,
              srn,
              ua23,
              landOrPropertyTransactions(index.value - 1).optDisposedPropertyTransaction,
              landOrProperty.optDisposeAnyLandOrProperty
            )
          }
          triedUA.flatten
      }
    } yield resultUA
  }

  private def buildOptDisposedTransactionUA(
    index: Max5000,
    srn: Srn,
    userAnswers: UserAnswers,
    optDisposedPropertyTransaction: Option[Seq[DisposedPropertyTransaction]],
    optDisposeAnyLandOrProperty: Option[Boolean]
  ): Try[UserAnswers] = {

    val initialUserAnswersOfDisposal =
      if (optDisposeAnyLandOrProperty.isEmpty) {
        Try(userAnswers)
      } else {
        userAnswers.set(LandOrPropertyDisposalPage(srn), optDisposeAnyLandOrProperty.getOrElse(false))
      }

    optDisposedPropertyTransaction
      .map(
        disposedPropertyTransactions => {
          for {
            disposalIndexes <- buildIndexesForMax50(disposedPropertyTransactions.size)
            resultDisposalUA <- disposalIndexes.foldLeft(initialUserAnswersOfDisposal) {
              case (disposalUA, disposalIndex) =>
                val disposedPropertyTransaction = disposedPropertyTransactions(disposalIndex.value - 1)
                val methodOfDisposal = disposedPropertyTransaction.methodOfDisposal match {
                  case HowDisposed.Sold.name => HowDisposed.Sold
                  case HowDisposed.Transferred.name => HowDisposed.Transferred
                  case HowDisposed.Other.name =>
                    HowDisposed.Other(disposedPropertyTransaction.optOtherMethod.get)
                }

                val howWasPropertyDisposed = HowWasPropertyDisposedOfPage(srn, index, disposalIndex) -> methodOfDisposal
                val portionStillHeld = LandOrPropertyStillHeldPage(srn, index, disposalIndex) -> disposedPropertyTransaction.portionStillHeld
                val optWhenWasPropertySold = disposedPropertyTransaction.optDateOfSale.map(
                  date => WhenWasPropertySoldPage(srn, index, disposalIndex) -> date
                )
                val optConnectedParty = disposedPropertyTransaction.optConnectedPartyStatus.map(
                  status => LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex) -> status
                )
                val optSaleProceeds = disposedPropertyTransaction.optSaleProceeds.map(
                  saleProceeds => TotalProceedsSaleLandPropertyPage(srn, index, disposalIndex) -> Money(saleProceeds)
                )

                val optIndepValuation = disposedPropertyTransaction.optIndepValuationSupport.map(
                  indepValuation => DisposalIndependentValuationPage(srn, index, disposalIndex) -> indepValuation
                )

                val optLandOrPropertyDisposedType = disposedPropertyTransaction.optPropertyAcquiredFrom.map(
                  prop => {
                    WhoPurchasedLandOrPropertyPage(srn, index, disposalIndex) -> prop.identityType
                  }
                )
                val optIndividualTuple = disposedPropertyTransaction.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.Individual)
                  .map(
                    prop => {
                      val name = LandOrPropertyIndividualBuyerNamePage(srn, index, disposalIndex) -> disposedPropertyTransaction.optNameOfPurchaser.get
                      val yesNoValue = prop.idNumber
                        .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                        .getOrElse(
                          ConditionalYesNo.no[String, Nino](
                            prop.reasonNoIdNumber.get
                          )
                        )

                      (name, IndividualBuyerNinoNumberPage(srn, index, disposalIndex) -> yesNoValue)
                    }
                  )

                val optUKCompanyTuple = disposedPropertyTransaction.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.UKCompany)
                  .map(
                    prop => {
                      val name = CompanyBuyerNamePage(srn, index, disposalIndex) -> disposedPropertyTransaction.optNameOfPurchaser.get
                      val yesNoValue = prop.idNumber
                        .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                        .getOrElse(
                          ConditionalYesNo.no[String, Crn](
                            prop.reasonNoIdNumber.get
                          )
                        )
                      (name, CompanyBuyerCrnPage(srn, index, disposalIndex) -> yesNoValue)
                    }
                  )

                val optUKPartnershipTuple = disposedPropertyTransaction.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.UKPartnership)
                  .map(
                    prop => {
                      val name = PartnershipBuyerNamePage(srn, index, disposalIndex) -> disposedPropertyTransaction.optNameOfPurchaser.get
                      val yesNoValue = prop.idNumber
                        .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                        .getOrElse(
                          ConditionalYesNo.no[String, Utr](
                            prop.reasonNoIdNumber.get
                          )
                        )

                      (name, PartnershipBuyerUtrPage(srn, index, disposalIndex) -> yesNoValue)
                    }
                  )

                val optOther = disposedPropertyTransaction.optPropertyAcquiredFrom
                  .filter(prop => prop.identityType == IdentityType.Other)
                  .map(
                    prop => {
                      OtherBuyerDetailsPage(srn, index, disposalIndex) -> RecipientDetails(
                        disposedPropertyTransaction.optNameOfPurchaser.get,
                        prop.otherDescription.get
                      )
                    }
                  )
                for {
                  disposalUA0 <- disposalUA
                  disposalUA1 <- disposalUA0
                    .set(LandPropertyDisposalCompletedPage(srn, index, disposalIndex), SectionCompleted)
                  disposalUA2 <- disposalUA1.set(howWasPropertyDisposed._1, howWasPropertyDisposed._2)
                  disposalUA3 <- disposalUA2.set(portionStillHeld._1, portionStillHeld._2)
                  disposalUA4 <- optWhenWasPropertySold
                    .map(t => disposalUA3.set(t._1, t._2))
                    .getOrElse(Try(disposalUA3))
                  disposalUA5 <- optSaleProceeds
                    .map(t => disposalUA4.set(t._1, t._2))
                    .getOrElse(Try(disposalUA4))

                  disposalUA6 <- optIndepValuation
                    .map(t => disposalUA5.set(t._1, t._2))
                    .getOrElse(Try(disposalUA5))
                  disposalUA7 <- optLandOrPropertyDisposedType
                    .map(t => disposalUA6.set(t._1, t._2))
                    .getOrElse(Try(disposalUA6))

                  disposalUA8 <- optIndividualTuple
                    .map(t => disposalUA7.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA7))
                  disposalUA9 <- optIndividualTuple
                    .map(t => disposalUA8.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA8))

                  disposalUA10 <- optUKCompanyTuple
                    .map(t => disposalUA9.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA9))
                  disposalUA11 <- optUKCompanyTuple
                    .map(t => disposalUA10.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA10))

                  disposalUA12 <- optUKPartnershipTuple
                    .map(t => disposalUA11.set(t._1._1, t._1._2))
                    .getOrElse(Try(disposalUA11))
                  disposalUA13 <- optUKPartnershipTuple
                    .map(t => disposalUA12.set(t._2._1, t._2._2))
                    .getOrElse(Try(disposalUA12))
                  disposalUA14 <- optConnectedParty
                    .map(t => disposalUA13.set(t._1, t._2))
                    .getOrElse(Try(disposalUA13))
                  disposalUA15 <- optOther.map(t => disposalUA14.set(t._1, t._2)).getOrElse(Try(disposalUA14))
                } yield disposalUA15
            }
          } yield resultDisposalUA
        }
      )
      .getOrElse(initialUserAnswersOfDisposal)
  }

  private def buildLandRegistryTitleNumberDetail(srn: Srn, index: Max5000, propertyDetails: PropertyDetails) = {
    val yesOrNo = propertyDetails.landRegistryTitleNumberKey
    val value = propertyDetails.landRegistryTitleNumberValue
    val yesNoValue = if (yesOrNo) {
      ConditionalYesNo.yes[String, String](
        value
      )
    } else {
      ConditionalYesNo.no[String, String](
        value
      )
    }
    LandRegistryTitleNumberPage(srn, index) -> yesNoValue
  }

  private def buildOptNoneTransferRelatedDetails(
    methodOfHolding: SchemeHoldLandProperty,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(Boolean, LocalDate)] = Option.when(methodOfHolding != Transfer) {
    val landPropertyIndependentValuation =
      request.userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).get
    val landOrPropertyAcquire =
      request.userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)).get
    (
      landPropertyIndependentValuation,
      landOrPropertyAcquire
    )
  }

  private def buildOptAcquisitionRelatedDetails(
    methodOfHolding: SchemeHoldLandProperty,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(String, Boolean, PropertyAcquiredFrom)] =
    Option
      .when(methodOfHolding == Acquisition) {
        val receivedLandType =
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)).get
        val sellerConnectedParty =
          request.userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, index)).get

        receivedLandType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(LandPropertyIndividualSellersNamePage(srn, index)),
              request.userAnswers.get(IndividualSellerNiPage(srn, index)).map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanySellerNamePage(srn, index)),
              request.userAnswers
                .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipSellerNamePage(srn, index)),
              request.userAnswers
                .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
              .map(
                other =>
                  (
                    other.name,
                    sellerConnectedParty,
                    PropertyAcquiredFrom(
                      identityType = receivedLandType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )

        }
      }
      .flatten

  private def buildOptLandOrPropertyLeasedDetails(
    landOrPropertyLeased: Boolean,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[LeaseDetails] =
    Option.when(landOrPropertyLeased) {
      val leaseDetails =
        request.userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index))
      val leaseConnectedParty = request.userAnswers.get(IsLesseeConnectedPartyPage(srn, index))
      LeaseDetails(
        optLesseeName = leaseDetails.map(_._1),
        optLeaseGrantDate = leaseDetails.map(_._3),
        optAnnualLeaseAmount = leaseDetails.map(_._2.value),
        optConnectedPartyStatus = leaseConnectedParty.map(_.booleanValue())
      )
    }

  private def buildOptDisposedPropertyTransactions(
    srn: Srn,
    landOrPropertyIndex: Refined[Int, OneTo5000]
  )(
    implicit request: DataRequest[_]
  ): Seq[DisposedPropertyTransaction] =
    request.userAnswers
      .map(
        HowWasPropertyDisposedOfPages(srn, landOrPropertyIndex)
      )
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo50](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              howWasPropertyDisposed <- request.userAnswers.get(
                HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, index)
              )
              landOrPropertyStillHeld <- request.userAnswers.get(
                LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, index)
              )

            } yield {
              val optSoldRelatedDetails =
                buildOptPropertyAcquiredFrom(howWasPropertyDisposed, srn, landOrPropertyIndex, index)

              DisposedPropertyTransaction(
                methodOfDisposal = howWasPropertyDisposed.name,
                optOtherMethod = howWasPropertyDisposed match {
                  case Other(details) => Some(details)
                  case _ => None
                },
                optDateOfSale = Option.when(howWasPropertyDisposed == Sold)(
                  request.userAnswers
                    .get(
                      WhenWasPropertySoldPage(srn, landOrPropertyIndex, index)
                    )
                    .get
                ),
                optNameOfPurchaser = optSoldRelatedDetails.map(_._1),
                optPropertyAcquiredFrom = optSoldRelatedDetails.map(_._2),
                optSaleProceeds = Option.when(howWasPropertyDisposed == Sold)(
                  request.userAnswers
                    .get(
                      TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, index)
                    )
                    .get
                    .value
                ),
                optConnectedPartyStatus = Option.when(howWasPropertyDisposed == Sold)(
                  request.userAnswers
                    .get(
                      LandOrPropertyDisposalBuyerConnectedPartyPage(srn, landOrPropertyIndex, index)
                    )
                    .get
                ),
                optIndepValuationSupport = Option.when(howWasPropertyDisposed == Sold)(
                  request.userAnswers
                    .get(
                      DisposalIndependentValuationPage(srn, landOrPropertyIndex, index)
                    )
                    .get
                ),
                portionStillHeld = landOrPropertyStillHeld
              )
            }
        }
      }

  private def buildOptPropertyAcquiredFrom(
    howWasPropertyDisposed: HowDisposed,
    srn: Srn,
    landOrPropertyIndex: Refined[Int, OneTo5000],
    disposalIndex: Refined[Int, OneTo50]
  )(implicit request: DataRequest[_]): Option[(String, PropertyAcquiredFrom)] =
    Option
      .when(howWasPropertyDisposed == Sold) {
        val landOrPropertyDisposedType =
          request.userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex)).get

        landOrPropertyDisposedType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
              request.userAnswers
                .get(IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
              request.userAnswers
                .get(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)),
              request.userAnswers
                .get(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = landOrPropertyDisposedType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex))
              .map(
                other =>
                  (
                    other.name,
                    PropertyAcquiredFrom(
                      identityType = landOrPropertyDisposedType,
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
